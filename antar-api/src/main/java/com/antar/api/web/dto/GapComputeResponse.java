package com.antar.api.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record GapComputeResponse(
        BigDecimal totalBill,
        BigDecimal payout,
        BigDecimal gap,
        BigDecimal gapPercentOfBill,
        List<DeductionTraceResponse> deductions) {
}
