package com.antar.api.catalogue.web.dto;

import com.antar.api.catalogue.persistence.AvailabilityStatus;
import com.antar.api.catalogue.persistence.ProductCategory;
import com.antar.api.catalogue.persistence.Segment;

/**
 * hasAnyVerifiedTerms from the design doc is deliberately omitted here - it depends on
 * version_terms_template, which does not exist yet (design-doc step 5, out of scope).
 * A hardcoded false would look like a real check when it isn't one.
 */
public record ProductSummaryResponse(
        Long id,
        String productName,
        ProductCategory productCategory,
        Segment segment,
        AvailabilityStatus availabilityStatus,
        int versionCount,
        /** UIN of the version with an open-ended (null) effective_to. Null if none. */
        String currentUin) {
}
