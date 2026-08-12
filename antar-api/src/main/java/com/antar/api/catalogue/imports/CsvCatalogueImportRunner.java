package com.antar.api.catalogue.imports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs the catalogue CSV import on application startup, gated by
 * antar.catalogue.import-on-startup (default false everywhere - see
 * docs/data/README.md "Import behaviour" for why). This is deliberately the
 * only entry point into CatalogueImportService in this codebase: there is no
 * admin HTTP endpoint that triggers an import, so it cannot be hit accidentally
 * or abused.
 */
@Component
public class CsvCatalogueImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CsvCatalogueImportRunner.class);
    private static final String STARTUP_RUN_BY = "startup-import";

    private final CatalogueImportService importService;
    private final boolean importOnStartup;
    private final boolean importForce;

    public CsvCatalogueImportRunner(CatalogueImportService importService,
                                     @Value("${antar.catalogue.import-on-startup:false}") boolean importOnStartup,
                                     @Value("${antar.catalogue.import-force:false}") boolean importForce) {
        this.importService = importService;
        this.importOnStartup = importOnStartup;
        this.importForce = importForce;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!importOnStartup) {
            log.info("Catalogue CSV import skipped (antar.catalogue.import-on-startup=false)");
            return;
        }
        log.info("Running catalogue CSV import (force={})", importForce);
        importService.importAll(importForce, STARTUP_RUN_BY)
                .forEach(result -> log.info(
                        "Catalogue import [{}]: inserted={} updated={} skipped={} wholeDatasetSkipped={}",
                        result.dataset(), result.rowsInserted(), result.rowsUpdated(), result.rowsSkipped(),
                        result.skippedWholeDataset()));
    }
}
