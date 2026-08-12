package com.antar.api.catalogue.persistence;

/** Can you buy it today? Separate question from {@link VerificationStatus}. */
public enum AvailabilityStatus {
    OPEN_TO_NEW,
    CLOSED_TO_NEW,
    WITHDRAWN,
    /** Not determined. The honest default - never assume OPEN_TO_NEW. */
    UNKNOWN
}
