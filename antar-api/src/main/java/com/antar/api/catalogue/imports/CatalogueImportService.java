package com.antar.api.catalogue.imports;

import com.antar.api.catalogue.persistence.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Upserts the health-insurer catalogue from the CSVs shipped at classpath:data/*.csv
 * (built from docs/data/*.csv - see docs/data/README.md). Idempotent: re-running
 * against the same files does not create duplicate rows.
 *
 * Rows whose source column is the literal placeholder "EXAMPLE - REPLACE" are never
 * persisted - they exist only to show a human editor the column shapes.
 */
@Service
public class CatalogueImportService {

    private static final Logger log = LoggerFactory.getLogger(CatalogueImportService.class);
    static final String EXAMPLE_SOURCE_MARKER = "EXAMPLE - REPLACE";

    private final InsurerRepository insurerRepository;
    private final InsuranceProductRepository productRepository;
    private final ProductVersionRepository versionRepository;
    private final DataImportRunRepository importRunRepository;

    public CatalogueImportService(InsurerRepository insurerRepository,
                                   InsuranceProductRepository productRepository,
                                   ProductVersionRepository versionRepository,
                                   DataImportRunRepository importRunRepository) {
        this.insurerRepository = insurerRepository;
        this.productRepository = productRepository;
        this.versionRepository = versionRepository;
        this.importRunRepository = importRunRepository;
    }

    /**
     * Imports insurers, then products, then versions - in that order, since each
     * later dataset resolves its parent by natural key. Each dataset is skipped
     * in its entirety if its table already has rows, unless force is true.
     */
    @Transactional
    public List<ImportResult> importAll(boolean force, String runBy) {
        ImportResult insurers = importInsurers(force, runBy);
        ImportResult products = importProducts(force, runBy);
        ImportResult versions = importVersions(force, runBy);
        return List.of(insurers, products, versions);
    }

    ImportResult importInsurers(boolean force, String runBy) {
        String sourceFile = "data/insurers.csv";
        if (!force && insurerRepository.count() > 0) {
            log.info("Skipping insurer import - table already populated and import-force is false");
            return ImportResult.wholeDatasetSkipped("insurer", sourceFile);
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        try (Reader reader = classpathReader(sourceFile);
             CSVParser parser = csvFormat().parse(reader)) {

            List<CSVRecord> records = parser.getRecords();

            for (CSVRecord row : records) {
                String source = row.get("source");
                if (isExampleRow(source)) {
                    skipped++;
                    continue;
                }

                String registrationNo = row.get("irdai_registration_no");
                Optional<InsurerEntity> existing = insurerRepository.findByIrdaiRegistrationNo(registrationNo);
                InsurerEntity entity = existing.orElseGet(InsurerEntity::new);

                entity.setIrdaiRegistrationNo(registrationNo);
                entity.setLegalName(row.get("legal_name"));
                entity.setDisplayName(row.get("display_name"));
                entity.setInsurerType(InsurerType.valueOf(row.get("insurer_type")));
                entity.setActive(blankTo(row.get("is_active"), "TRUE").equalsIgnoreCase("TRUE"));
                entity.setCeasedDate(parseDate(row.get("ceased_date")));
                entity.setSource(source);
                entity.setLastVerifiedDate(LocalDate.parse(row.get("last_verified_date")));

                insurerRepository.save(entity);
                if (existing.isPresent()) {
                    updated++;
                } else {
                    inserted++;
                }
            }

            // Second pass: successor links, resolved by registration number, once
            // every insurer in the file has been persisted (order in the CSV
            // shouldn't matter for a forward reference to a later row).
            for (CSVRecord row : records) {
                if (isExampleRow(row.get("source"))) {
                    continue;
                }
                String successorRegNo = row.get("succeeded_by_insurer_registration_no");
                if (successorRegNo == null || successorRegNo.isBlank()) {
                    continue;
                }
                Optional<InsurerEntity> insurer = insurerRepository.findByIrdaiRegistrationNo(row.get("irdai_registration_no"));
                Optional<InsurerEntity> successor = insurerRepository.findByIrdaiRegistrationNo(successorRegNo);
                if (insurer.isPresent() && successor.isPresent()) {
                    insurer.get().setSucceededByInsurerId(successor.get().getId());
                    insurerRepository.save(insurer.get());
                } else {
                    log.warn("Could not resolve successor {} for insurer {}", successorRegNo, row.get("irdai_registration_no"));
                }
            }
        } catch (IOException e) {
            throw new CatalogueImportException("Failed to read " + sourceFile, e);
        }

        ImportResult result = new ImportResult("insurer", sourceFile, inserted, updated, skipped, false);
        recordRun(result, runBy);
        return result;
    }

    ImportResult importProducts(boolean force, String runBy) {
        String sourceFile = "data/products.csv";
        if (!force && productRepository.count() > 0) {
            log.info("Skipping product import - table already populated and import-force is false");
            return ImportResult.wholeDatasetSkipped("insurance_product", sourceFile);
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        try (Reader reader = classpathReader(sourceFile);
             CSVParser parser = csvFormat().parse(reader)) {

            for (CSVRecord row : parser) {
                String source = row.get("source");
                if (isExampleRow(source)) {
                    skipped++;
                    continue;
                }

                Optional<InsurerEntity> insurer = insurerRepository.findByIrdaiRegistrationNo(row.get("insurer_registration_no"));
                if (insurer.isEmpty()) {
                    log.warn("Skipping product row - unknown insurer_registration_no {}", row.get("insurer_registration_no"));
                    skipped++;
                    continue;
                }

                String productName = row.get("product_name");
                Segment segment = Segment.valueOf(row.get("segment"));
                Optional<InsuranceProductEntity> existing = productRepository
                        .findByInsurerIdAndProductNameAndSegment(insurer.get().getId(), productName, segment);
                InsuranceProductEntity entity = existing.orElseGet(InsuranceProductEntity::new);

                entity.setInsurerId(insurer.get().getId());
                entity.setProductName(productName);
                entity.setProductCategory(ProductCategory.valueOf(row.get("product_category")));
                entity.setSegment(segment);
                entity.setAvailabilityStatus(parseAvailability(row.get("availability_status")));
                entity.setFirstClearedDate(parseDate(row.get("first_cleared_date")));
                entity.setNotes(blankToNull(row.get("notes")));
                entity.setSource(source);
                entity.setLastVerifiedDate(LocalDate.parse(row.get("last_verified_date")));

                productRepository.save(entity);
                if (existing.isPresent()) {
                    updated++;
                } else {
                    inserted++;
                }
            }
        } catch (IOException e) {
            throw new CatalogueImportException("Failed to read " + sourceFile, e);
        }

        ImportResult result = new ImportResult("insurance_product", sourceFile, inserted, updated, skipped, false);
        recordRun(result, runBy);
        return result;
    }

    ImportResult importVersions(boolean force, String runBy) {
        String sourceFile = "data/product-versions.csv";
        if (!force && versionRepository.count() > 0) {
            log.info("Skipping product version import - table already populated and import-force is false");
            return ImportResult.wholeDatasetSkipped("product_version", sourceFile);
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        try (Reader reader = classpathReader(sourceFile);
             CSVParser parser = csvFormat().parse(reader)) {

            for (CSVRecord row : parser) {
                String source = row.get("source");
                if (isExampleRow(source)) {
                    skipped++;
                    continue;
                }

                Optional<InsurerEntity> insurer = insurerRepository.findByIrdaiRegistrationNo(row.get("insurer_registration_no"));
                if (insurer.isEmpty()) {
                    log.warn("Skipping product-version row - unknown insurer_registration_no {}", row.get("insurer_registration_no"));
                    skipped++;
                    continue;
                }

                Segment segment = Segment.valueOf(row.get("segment"));
                Optional<InsuranceProductEntity> product = productRepository
                        .findByInsurerIdAndProductNameAndSegment(insurer.get().getId(), row.get("product_name"), segment);
                if (product.isEmpty()) {
                    log.warn("Skipping product-version row - unknown product {}/{}/{}",
                            row.get("insurer_registration_no"), row.get("product_name"), segment);
                    skipped++;
                    continue;
                }

                String uin = row.get("uin");
                Optional<ProductVersionEntity> existing = versionRepository.findByUin(uin);
                ProductVersionEntity entity = existing.orElseGet(ProductVersionEntity::new);

                entity.setProductId(product.get().getId());
                entity.setUin(uin);
                entity.setVersionLabel(blankToNull(row.get("version_label")));
                entity.setIrdaiClearedDate(parseDate(row.get("irdai_cleared_date")));
                entity.setEffectiveFrom(parseDate(row.get("effective_from")));
                entity.setEffectiveTo(parseDate(row.get("effective_to")));
                entity.setVerificationStatus(parseVerification(row.get("verification_status")));
                entity.setSourceUrl(row.get("source_url"));
                entity.setWordingPdfUrl(blankToNull(row.get("wording_pdf_url")));
                entity.setSource(source);
                entity.setLastVerifiedDate(LocalDate.parse(row.get("last_verified_date")));

                versionRepository.save(entity);
                if (existing.isPresent()) {
                    updated++;
                } else {
                    inserted++;
                }
            }
        } catch (IOException e) {
            throw new CatalogueImportException("Failed to read " + sourceFile, e);
        }

        ImportResult result = new ImportResult("product_version", sourceFile, inserted, updated, skipped, false);
        recordRun(result, runBy);
        return result;
    }

    private void recordRun(ImportResult result, String runBy) {
        importRunRepository.save(new DataImportRunEntity(
                result.dataset(), result.sourceFile(), result.rowsInserted(), result.rowsUpdated(),
                result.rowsSkipped(), runBy));
    }

    private boolean isExampleRow(String source) {
        return EXAMPLE_SOURCE_MARKER.equals(source == null ? null : source.trim());
    }

    private Reader classpathReader(String classpathLocation) throws IOException {
        return new InputStreamReader(new ClassPathResource(classpathLocation).getInputStream(), StandardCharsets.UTF_8);
    }

    private CSVFormat csvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();
    }

    private LocalDate parseDate(String value) {
        return (value == null || value.isBlank()) ? null : LocalDate.parse(value);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String blankTo(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private AvailabilityStatus parseAvailability(String value) {
        return (value == null || value.isBlank()) ? AvailabilityStatus.UNKNOWN : AvailabilityStatus.valueOf(value);
    }

    private VerificationStatus parseVerification(String value) {
        return (value == null || value.isBlank()) ? VerificationStatus.UNVERIFIED : VerificationStatus.valueOf(value);
    }
}
