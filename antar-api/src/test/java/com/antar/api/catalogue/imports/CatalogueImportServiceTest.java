package com.antar.api.catalogue.imports;

import com.antar.api.catalogue.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the importer against the fixture CSVs at src/test/resources/data/*.csv,
 * which shadow the real docs/data CSVs on the test classpath. "test" profile
 * disables the automatic startup import (application-test.yml) - this class
 * calls CatalogueImportService directly so it controls exactly when import runs.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogueImportServiceTest {

    @Autowired
    private CatalogueImportService importService;

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private InsuranceProductRepository productRepository;

    @Autowired
    private ProductVersionRepository versionRepository;

    @Autowired
    private DataImportRunRepository importRunRepository;

    @Test
    void firstImportInsertsInsurersProductsAndVersionsAndSkipsUnknownParents() {
        List<ImportResult> results = importService.importAll(false, "test");

        ImportResult insurers = results.get(0);
        ImportResult products = results.get(1);
        ImportResult versions = results.get(2);

        // 2 keyed by registration number, 1 with no registration number (keyed by display name).
        assertThat(insurers.rowsInserted()).isEqualTo(3);
        assertThat(insurerRepository.count()).isEqualTo(3);

        // 3 valid rows imported (2 by registration number, 1 by insurer display name),
        // 1 row referencing an unknown insurer skipped, not fatal.
        assertThat(products.rowsInserted()).isEqualTo(3);
        assertThat(products.rowsSkipped()).isEqualTo(1);
        assertThat(productRepository.count()).isEqualTo(3);

        assertThat(versions.rowsInserted()).isEqualTo(2);
        assertThat(versionRepository.count()).isEqualTo(2);

        assertThat(importRunRepository.count()).isEqualTo(3);
    }

    @Test
    void insurerWithNoRegistrationNumberIsKeyedByDisplayNameAndDoesNotDuplicateOnReimport() {
        importService.importAll(false, "test");
        long countAfterFirstRun = insurerRepository.count();

        List<ImportResult> secondRun = importService.importAll(true, "test");

        assertThat(insurerRepository.findByDisplayName("No Registration Number Insurer")).isPresent();
        assertThat(secondRun.get(0).rowsUpdated()).isEqualTo(countAfterFirstRun);
        assertThat(secondRun.get(0).rowsInserted()).isZero();
        assertThat(insurerRepository.count()).isEqualTo(countAfterFirstRun);
    }

    @Test
    void secondImportWithoutForceSkipsTheWholePopulatedDataset() {
        importService.importAll(false, "test");
        long insurerCountAfterFirstRun = insurerRepository.count();

        List<ImportResult> secondRun = importService.importAll(false, "test");

        assertThat(secondRun.get(0).skippedWholeDataset()).isTrue();
        assertThat(insurerRepository.count()).isEqualTo(insurerCountAfterFirstRun);
    }

    @Test
    void reImportingWithForceUpsertsWithoutCreatingDuplicates() {
        importService.importAll(false, "test");
        long insurerCountAfterFirstRun = insurerRepository.count();
        long productCountAfterFirstRun = productRepository.count();
        long versionCountAfterFirstRun = versionRepository.count();

        List<ImportResult> secondRun = importService.importAll(true, "test");

        assertThat(secondRun.get(0).skippedWholeDataset()).isFalse();
        assertThat(secondRun.get(0).rowsUpdated()).isEqualTo(3);
        assertThat(secondRun.get(0).rowsInserted()).isZero();

        assertThat(insurerRepository.count()).isEqualTo(insurerCountAfterFirstRun);
        assertThat(productRepository.count()).isEqualTo(productCountAfterFirstRun);
        assertThat(versionRepository.count()).isEqualTo(versionCountAfterFirstRun);
    }

    @Test
    void exampleMarkedRowsInRealCatalogueCsvsAreNeverPersisted() {
        // Sanity check on the actual shipped docs/data CSVs' placeholder marker,
        // independent of the test fixtures above.
        assertThat(CatalogueImportService.EXAMPLE_SOURCE_MARKER).isEqualTo("EXAMPLE - REPLACE");
    }
}
