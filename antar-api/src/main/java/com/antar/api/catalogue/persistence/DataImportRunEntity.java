package com.antar.api.catalogue.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "data_import_run")
public class DataImportRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset", nullable = false, length = 50)
    private String dataset;

    @Column(name = "source_file", nullable = false, length = 300)
    private String sourceFile;

    @Column(name = "rows_inserted", nullable = false)
    private int rowsInserted;

    @Column(name = "rows_updated", nullable = false)
    private int rowsUpdated;

    @Column(name = "rows_skipped", nullable = false)
    private int rowsSkipped;

    @Column(name = "run_by", nullable = false, length = 100)
    private String runBy;

    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;

    public DataImportRunEntity() {
        // JPA requires a no-arg constructor
    }

    public DataImportRunEntity(String dataset, String sourceFile, int rowsInserted, int rowsUpdated,
                                int rowsSkipped, String runBy) {
        this.dataset = dataset;
        this.sourceFile = sourceFile;
        this.rowsInserted = rowsInserted;
        this.rowsUpdated = rowsUpdated;
        this.rowsSkipped = rowsSkipped;
        this.runBy = runBy;
    }

    @PrePersist
    void onCreate() {
        this.runAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public String getDataset() { return dataset; }
    public String getSourceFile() { return sourceFile; }
    public int getRowsInserted() { return rowsInserted; }
    public int getRowsUpdated() { return rowsUpdated; }
    public int getRowsSkipped() { return rowsSkipped; }
    public String getRunBy() { return runBy; }
    public LocalDateTime getRunAt() { return runAt; }
}
