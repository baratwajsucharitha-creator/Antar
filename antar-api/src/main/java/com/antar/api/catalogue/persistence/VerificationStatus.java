package com.antar.api.catalogue.persistence;

/** How confident is this record? Separate question from {@link AvailabilityStatus}. */
public enum VerificationStatus {
    VERIFIED_IRDAI,
    VERIFIED_INSURER,
    /** Recorded but not confirmed. The honest default. */
    UNVERIFIED
}
