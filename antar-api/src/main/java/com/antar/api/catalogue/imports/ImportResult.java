package com.antar.api.catalogue.imports;

/** Outcome of importing one CSV dataset. */
public record ImportResult(String dataset, String sourceFile, int rowsInserted, int rowsUpdated, int rowsSkipped,
                            boolean skippedWholeDataset) {

    static ImportResult wholeDatasetSkipped(String dataset, String sourceFile) {
        return new ImportResult(dataset, sourceFile, 0, 0, 0, true);
    }
}
