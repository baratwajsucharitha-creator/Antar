package com.antar.api.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SubLimitDto(
        @NotBlank String procedureCode,
        @NotNull @DecimalMin("0.00") BigDecimal cap) {
}
