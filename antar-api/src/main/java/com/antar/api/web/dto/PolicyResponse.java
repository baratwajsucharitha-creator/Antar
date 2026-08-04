package com.antar.api.web.dto;

import com.antar.engine.model.CoPayOrder;

import java.math.BigDecimal;
import java.util.List;

public record PolicyResponse(
        Long id,
        String insurerName,
        String productName,
        BigDecimal sumInsured,
        BigDecimal remainingSumInsured,
        BigDecimal eligibleRoomRentPerDay,
        BigDecimal coPayPercent,
        CoPayOrder coPayOrder,
        List<SubLimitDto> subLimits) {
}
