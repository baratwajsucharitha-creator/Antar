package com.antar.api.web.dto;

import com.antar.engine.model.CoPayOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record PolicyRequest(
        @NotBlank String insurerName,
        @NotBlank String productName,
        @NotNull @DecimalMin("1.00") BigDecimal sumInsured,
        @NotNull @DecimalMin("0.00") BigDecimal remainingSumInsured,
        @NotNull @DecimalMin(value = "0.0000", inclusive = false) @DecimalMax("1.0000")
        BigDecimal roomRentPercentOfSumInsuredPerDay,
        boolean proportionPharmacy,
        @NotNull @DecimalMin("0.0000") @DecimalMax("1.0000") BigDecimal coPayPercent,
        @NotNull CoPayOrder coPayOrder,
        @Valid List<SubLimitDto> subLimits) {
}
