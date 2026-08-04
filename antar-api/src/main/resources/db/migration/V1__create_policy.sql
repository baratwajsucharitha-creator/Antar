-- H2 in MSSQLServer compatibility mode.
-- Week 3 adds a SQL Server specific migration path via spring.flyway.locations.

CREATE TABLE policy (
    id                      BIGINT        NOT NULL IDENTITY PRIMARY KEY,
    insurer_name            VARCHAR(120)  NOT NULL,
    product_name            VARCHAR(120)  NOT NULL,
    sum_insured             DECIMAL(15,2) NOT NULL,
    remaining_sum_insured   DECIMAL(15,2) NOT NULL,
    room_rent_percent       DECIMAL(6,4)  NOT NULL,
    proportion_pharmacy     BIT           NOT NULL,
    co_pay_percent          DECIMAL(5,4)  NOT NULL,
    co_pay_order            VARCHAR(30)   NOT NULL
);

CREATE TABLE policy_sub_limit (
    policy_id       BIGINT        NOT NULL,
    procedure_code  VARCHAR(60)   NOT NULL,
    cap_amount      DECIMAL(15,2) NOT NULL,
    CONSTRAINT fk_sub_limit_policy FOREIGN KEY (policy_id) REFERENCES policy (id)
);

CREATE INDEX ix_policy_sub_limit_policy ON policy_sub_limit (policy_id);
