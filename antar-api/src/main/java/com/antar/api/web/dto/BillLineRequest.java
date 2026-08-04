package com.antar.api.web.dto;

import com.antar.engine.model.BillCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * The category is the engine's own enum rather than a duplicate.
 * It is a stable, published part of the domain vocabulary - mirroring it here
 * would add a mapping layer with no decoupling benefit.
 */
public record BillLineRequest(
        @NotBlank String description,
        @NotNull BillCategory category,
        @NotNull @DecimalMin("0.00") BigDecimal amount) {
}
