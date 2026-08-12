-- Reference data (steps 1-2 of Antar_Data_Layer_Design_v2.md). NOT owner-scoped:
-- publicly readable and cacheable, deliberately unlike the owner-scoped `policy`
-- table. Populated by a startup-conditional CSV import (off by default in Azure -
-- see antar.catalogue.import-on-startup), never by an unguarded endpoint.
--
-- version_terms_template and the policy.product_version_id link are deliberately
-- NOT part of this migration - they are design-doc step 5 and depend on manually
-- read policy wordings / an actual version-selection flow that doesn't exist yet.

CREATE TABLE insurer (
    id                      BIGINT       NOT NULL IDENTITY PRIMARY KEY,
    irdai_registration_no   VARCHAR(20)  NOT NULL,
    legal_name              VARCHAR(200) NOT NULL,
    display_name            VARCHAR(120) NOT NULL,
    insurer_type            VARCHAR(30)  NOT NULL,  -- STANDALONE_HEALTH | GENERAL
    is_active               BIT          NOT NULL DEFAULT 1,
    -- Set when an insurer merges or exits. Their products stay queryable.
    ceased_date             DATE         NULL,
    succeeded_by_insurer_id BIGINT       NULL,      -- e.g. Apollo Munich -> HDFC ERGO
    source                  VARCHAR(300) NOT NULL,
    last_verified_date      DATE         NOT NULL,
    created_date            DATETIME2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date            DATETIME2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_insurer_registration UNIQUE (irdai_registration_no),
    CONSTRAINT fk_insurer_successor FOREIGN KEY (succeeded_by_insurer_id) REFERENCES insurer (id)
);

CREATE INDEX ix_insurer_active ON insurer (is_active, display_name);


-- The product lineage: "Star Family Health Optima" as a continuing thing,
-- independent of which version is in force.
CREATE TABLE insurance_product (
    id                  BIGINT       NOT NULL IDENTITY PRIMARY KEY,
    insurer_id          BIGINT       NOT NULL,
    product_name        VARCHAR(200) NOT NULL,
    product_category    VARCHAR(40)  NOT NULL,  -- INDEMNITY | TOP_UP | SUPER_TOP_UP
                                                 -- | FIXED_BENEFIT | CRITICAL_ILLNESS
                                                 -- | PERSONAL_ACCIDENT | GOVERNMENT_SCHEME
    segment             VARCHAR(20)  NOT NULL,  -- RETAIL | GROUP
    availability_status VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    first_cleared_date  DATE         NULL,
    notes               VARCHAR(500) NULL,      -- e.g. renamed after a merger
    source              VARCHAR(300) NOT NULL,
    last_verified_date  DATE         NOT NULL,
    created_date        DATETIME2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date        DATETIME2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_insurer FOREIGN KEY (insurer_id) REFERENCES insurer (id),
    -- Same insurer can offer a retail and a group product under the same name;
    -- segment makes the natural key exact and lets the CSV import upsert safely.
    CONSTRAINT uq_product_natural UNIQUE (insurer_id, product_name, segment)
);

CREATE INDEX ix_product_insurer ON insurance_product (insurer_id, product_name);
CREATE INDEX ix_product_availability ON insurance_product (availability_status, product_name);


-- One row per UIN. This is what a policyholder's policy actually points at.
CREATE TABLE product_version (
    id                  BIGINT       NOT NULL IDENTITY PRIMARY KEY,
    product_id          BIGINT       NOT NULL,
    uin                 VARCHAR(50)  NOT NULL,
    version_label       VARCHAR(20)  NULL,      -- 'V01', 'V02' - parsed from the UIN
    irdai_cleared_date  DATE         NULL,
    -- The window during which a policy bought new would be on this version.
    effective_from      DATE         NULL,
    effective_to        DATE         NULL,      -- null = still the current version
    verification_status VARCHAR(30)  NOT NULL DEFAULT 'UNVERIFIED',
    source_url          VARCHAR(500) NOT NULL,
    wording_pdf_url     VARCHAR(500) NULL,
    source              VARCHAR(300) NOT NULL,
    last_verified_date  DATE         NOT NULL,
    created_date        DATETIME2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date        DATETIME2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_version_product FOREIGN KEY (product_id) REFERENCES insurance_product (id),
    CONSTRAINT uq_version_uin UNIQUE (uin)
);

CREATE INDEX ix_version_product_dates ON product_version (product_id, effective_from DESC);


-- Audit trail for CSV imports: which dataset, from which file, how many rows
-- inserted/updated/skipped, by whom, and when. One row per dataset per run.
CREATE TABLE data_import_run (
    id             BIGINT       NOT NULL IDENTITY PRIMARY KEY,
    dataset        VARCHAR(50)  NOT NULL,   -- 'insurer' | 'insurance_product' | 'product_version'
    source_file    VARCHAR(300) NOT NULL,
    rows_inserted  INT          NOT NULL,
    rows_updated   INT          NOT NULL,
    rows_skipped   INT          NOT NULL,
    run_by         VARCHAR(100) NOT NULL,
    run_at         DATETIME2    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
