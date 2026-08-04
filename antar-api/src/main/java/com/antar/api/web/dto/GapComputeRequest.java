package com.antar.api.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record GapComputeRequest(
        @NotNull Long policyId,
        @NotBlank String procedureCode,
        @Min(1) int daysAdmitted,
        @NotNull @DecimalMin("0.00") BigDecimal actualRoomRentPerDay,
        @NotEmpty @Valid List<BillLineRequest> lines) {
}
