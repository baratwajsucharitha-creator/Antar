package com.antar.engine.model;

/**
 * How each line of a hospital bill behaves under a policy.
 * The category, not the description, drives the calculation.
 */
public enum BillCategory {

    /** Room / bed charges. Capped at the eligible room rent. */
    ROOM(false),

    /** Doctor visits, nursing, OT, ICU, investigations. Reduced by the room proportion factor. */
    ASSOCIATED(true),

    /** Medicines and consumables. Most insurers do NOT proportion these. */
    PHARMACY(false),

    /** Implants, stents, lenses. Not proportioned. */
    IMPLANT(false),

    /** IRDAI Annexure I non-payable items. Removed entirely before anything else. */
    NON_PAYABLE(false);

    private final boolean proportionedByDefault;

    BillCategory(boolean proportionedByDefault) {
        this.proportionedByDefault = proportionedByDefault;
    }

    public boolean isProportionedByDefault() {
        return proportionedByDefault;
    }
}
