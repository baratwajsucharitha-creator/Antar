package com.antar.engine.model;

import java.util.List;
import java.util.Optional;

/** The clauses that drive the calculation, for one version of one insurer's product. */
public record PolicyTerms(
        RoomRentRule roomRentRule,
        List<SubLimit> subLimits,
        CoPayRule coPayRule) {

    public PolicyTerms {
        if (roomRentRule == null) {
            throw new IllegalArgumentException("roomRentRule must not be null");
        }
        if (coPayRule == null) {
            throw new IllegalArgumentException("coPayRule must not be null");
        }
        subLimits = subLimits == null ? List.of() : List.copyOf(subLimits);
    }

    public Optional<SubLimit> subLimitFor(String procedureCode) {
        return subLimits.stream()
                .filter(limit -> limit.procedureCode().equalsIgnoreCase(procedureCode))
                .findFirst();
    }
}
