package com.antar.api.catalogue.web.dto;

import com.antar.api.catalogue.persistence.VerificationStatus;

import java.time.LocalDate;

/**
 * hasVerifiedTerms from the design doc is deliberately omitted here - see
 * ProductSummaryResponse for why (version_terms_template does not exist yet).
 */
public record ProductVersionResponse(
        Long id,
        String uin,
        String versionLabel,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        VerificationStatus verificationStatus,
        String wordingPdfUrl) {
}
