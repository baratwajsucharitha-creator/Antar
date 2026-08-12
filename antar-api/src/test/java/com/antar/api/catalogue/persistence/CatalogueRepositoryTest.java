package com.antar.api.catalogue.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Keep the application.yml datasource (H2 in MSSQLServer mode) rather than
// @DataJpaTest's default embedded-DB replacement - the migrations use
// SQL-Server-flavoured syntax (IDENTITY, DATETIME2) that only that mode supports.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogueRepositoryTest {

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private InsuranceProductRepository productRepository;

    @Autowired
    private ProductVersionRepository versionRepository;

    private InsurerEntity persistInsurer(String regNo, InsurerType type) {
        InsurerEntity insurer = new InsurerEntity();
        insurer.setIrdaiRegistrationNo(regNo);
        insurer.setLegalName(regNo + " Legal Name Ltd");
        insurer.setDisplayName(regNo + " Display");
        insurer.setInsurerType(type);
        insurer.setSource("test");
        insurer.setLastVerifiedDate(LocalDate.of(2026, 8, 12));
        return insurerRepository.save(insurer);
    }

    private InsuranceProductEntity newProduct(Long insurerId, String name, Segment segment) {
        InsuranceProductEntity product = new InsuranceProductEntity();
        product.setInsurerId(insurerId);
        product.setProductName(name);
        product.setProductCategory(ProductCategory.INDEMNITY);
        product.setSegment(segment);
        product.setSource("test");
        product.setLastVerifiedDate(LocalDate.of(2026, 8, 12));
        return product;
    }

    @Test
    void insurerLookupByRegistrationNumberFindsAnExactMatch() {
        persistInsurer("REG-1", InsurerType.STANDALONE_HEALTH);

        Optional<InsurerEntity> found = insurerRepository.findByIrdaiRegistrationNo("REG-1");

        assertThat(found).isPresent();
        assertThat(found.get().getInsurerType()).isEqualTo(InsurerType.STANDALONE_HEALTH);
    }

    @Test
    void insurersCanBeFilteredByType() {
        persistInsurer("REG-SAHI", InsurerType.STANDALONE_HEALTH);
        persistInsurer("REG-GENERAL", InsurerType.GENERAL);

        List<InsurerEntity> sahi = insurerRepository.findByInsurerTypeOrderByDisplayName(InsurerType.STANDALONE_HEALTH);

        assertThat(sahi).extracting(InsurerEntity::getIrdaiRegistrationNo).containsExactly("REG-SAHI");
    }

    @Test
    void defaultAvailabilityStatusIsUnknownNeverAnOptimisticValue() {
        InsurerEntity insurer = persistInsurer("REG-2", InsurerType.STANDALONE_HEALTH);
        InsuranceProductEntity product = newProduct(insurer.getId(), "Some Product", Segment.RETAIL);

        InsuranceProductEntity saved = productRepository.save(product);

        assertThat(saved.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.UNKNOWN);
    }

    @Test
    void productsIncludeWithdrawnByDefaultQuery() {
        InsurerEntity insurer = persistInsurer("REG-3", InsurerType.STANDALONE_HEALTH);
        InsuranceProductEntity open = newProduct(insurer.getId(), "Open Product", Segment.RETAIL);
        open.setAvailabilityStatus(AvailabilityStatus.OPEN_TO_NEW);
        InsuranceProductEntity withdrawn = newProduct(insurer.getId(), "Withdrawn Product", Segment.RETAIL);
        withdrawn.setAvailabilityStatus(AvailabilityStatus.WITHDRAWN);
        productRepository.save(open);
        productRepository.save(withdrawn);

        List<InsuranceProductEntity> all = productRepository.findByInsurerIdOrderByProductName(insurer.getId());

        assertThat(all).extracting(InsuranceProductEntity::getProductName)
                .containsExactlyInAnyOrder("Open Product", "Withdrawn Product");
    }

    @Test
    void sameProductNameIsAllowedForRetailAndGroupSegments() {
        InsurerEntity insurer = persistInsurer("REG-4", InsurerType.STANDALONE_HEALTH);

        productRepository.save(newProduct(insurer.getId(), "Family Cover", Segment.RETAIL));
        productRepository.save(newProduct(insurer.getId(), "Family Cover", Segment.GROUP));
        productRepository.flush();

        assertThat(productRepository.findByInsurerIdOrderByProductName(insurer.getId())).hasSize(2);
    }

    @Test
    void naturalKeyConstraintRejectsADuplicateInsurerProductNameSegment() {
        InsurerEntity insurer = persistInsurer("REG-5", InsurerType.STANDALONE_HEALTH);
        productRepository.saveAndFlush(newProduct(insurer.getId(), "Duplicate Product", Segment.RETAIL));

        assertThatThrownBy(() ->
                productRepository.saveAndFlush(newProduct(insurer.getId(), "Duplicate Product", Segment.RETAIL))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void versionLookupByUinFindsAnExactMatch() {
        InsurerEntity insurer = persistInsurer("REG-6", InsurerType.STANDALONE_HEALTH);
        InsuranceProductEntity product = productRepository.save(newProduct(insurer.getId(), "Versioned Product", Segment.RETAIL));

        ProductVersionEntity version = new ProductVersionEntity();
        version.setProductId(product.getId());
        version.setUin("TESTUIN0001V012026");
        version.setSourceUrl("https://example.invalid/source");
        version.setSource("test");
        version.setLastVerifiedDate(LocalDate.of(2026, 8, 12));
        versionRepository.save(version);

        assertThat(versionRepository.findByUin("TESTUIN0001V012026")).isPresent();
        assertThat(versionRepository.findByUin("TESTUIN0001V012026").get().getVerificationStatus())
                .isEqualTo(VerificationStatus.UNVERIFIED);
    }
}
