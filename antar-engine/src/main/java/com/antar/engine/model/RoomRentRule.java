package com.antar.engine.model;

import java.math.BigDecimal;

/**
 * The room rent eligibility clause.
 *
 * @param percentOfSumInsuredPerDay e.g. 0.01 for "1% of sum insured per day"
 * @param proportionPharmacy        some insurers proportion pharmacy too; most do not
 */
public record RoomRentRule(BigDecimal percentOfSumInsuredPerDay, boolean proportionPharmacy) {

    public RoomRentRule {
        if (percentOfSumInsuredPerDay == null || percentOfSumInsuredPerDay.signum() <= 0) {
            throw new IllegalArgumentException("percentOfSumInsuredPerDay must be positive");
        }
    }

    public static RoomRentRule percentOfSumInsured(String percent) {
        return new RoomRentRule(new BigDecimal(percent), false);
    }

    /** The daily room rent this policy will actually accept. */
    public Money eligibleRoomRentPerDay(Money sumInsured) {
        return sumInsured.multiply(percentOfSumInsuredPerDay);
    }
}
