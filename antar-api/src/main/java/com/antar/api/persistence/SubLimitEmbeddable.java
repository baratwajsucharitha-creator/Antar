package com.antar.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class SubLimitEmbeddable {

    @Column(name = "procedure_code", nullable = false, length = 60)
    private String procedureCode;

    @Column(name = "cap_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal capAmount;

    protected SubLimitEmbeddable() {
        // required by JPA
    }

    public SubLimitEmbeddable(String procedureCode, BigDecimal capAmount) {
        this.procedureCode = procedureCode;
        this.capAmount = capAmount;
    }

    public String getProcedureCode() {
        return procedureCode;
    }

    public BigDecimal getCapAmount() {
        return capAmount;
    }
}
