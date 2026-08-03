package com.antar.engine.model;

/**
 * A policy as it stands right now.
 * remainingSumInsured moves with every claim in a family floater, so it is state, not a constant.
 */
public record Policy(
        String insurerName,
        String productName,
        Money sumInsured,
        Money remainingSumInsured,
        PolicyTerms terms) {

    public Policy {
        if (insurerName == null || insurerName.isBlank()) {
            throw new IllegalArgumentException("insurerName must not be blank");
        }
        if (sumInsured == null || remainingSumInsured == null) {
            throw new IllegalArgumentException("sum insured values must not be null");
        }
        if (remainingSumInsured.isGreaterThan(sumInsured)) {
            throw new IllegalArgumentException("remainingSumInsured cannot exceed sumInsured");
        }
        if (terms == null) {
            throw new IllegalArgumentException("terms must not be null");
        }
    }

    public Money eligibleRoomRentPerDay() {
        return terms.roomRentRule().eligibleRoomRentPerDay(sumInsured);
    }
}
