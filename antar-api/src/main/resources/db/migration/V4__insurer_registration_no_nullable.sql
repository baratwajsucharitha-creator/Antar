-- Verified IRDAI registration numbers are not available for the current seed
-- data (publicly known insurer/product names only - see docs/data/README.md).
-- irdai_registration_no becomes optional; the app must never invent one.
--
-- The design intent is a UNIQUE constraint that still allows any number of
-- NULL rows - normally a filtered/partial unique index
-- (CREATE UNIQUE INDEX ... WHERE irdai_registration_no IS NOT NULL), which
-- real SQL Server supports. H2's MSSQLServer compatibility mode does not
-- parse that WHERE clause at all (confirmed directly against the H2 driver),
-- so a single migration file that must run on both engines cannot use it.
-- This migration instead drops the UNIQUE constraint and creates a plain,
-- non-unique index for lookup performance only. Duplicate prevention for a
-- present registration number is enforced at the application layer
-- (CatalogueImportService looks up the existing row before inserting).
-- Before this table goes anywhere near production, replace this with a real
-- SQL-Server-specific migration variant that adds the filtered unique index -
-- see flyway-sqlserver / spring.flyway.locations for the vendor-specific
-- migration path this project already depends on but hasn't split out yet.

ALTER TABLE insurer DROP CONSTRAINT uq_insurer_registration;

ALTER TABLE insurer ALTER COLUMN irdai_registration_no VARCHAR(20) NULL;

CREATE INDEX ix_insurer_registration_no ON insurer (irdai_registration_no);
