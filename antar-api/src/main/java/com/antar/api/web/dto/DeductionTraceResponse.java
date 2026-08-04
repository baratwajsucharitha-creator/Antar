package com.antar.api.web.dto;

import java.math.BigDecimal;

public record DeductionTraceResponse(
        String clauseReference,
        String explanation,
        BigDecimal amountRemoved) {
}
