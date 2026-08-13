package com.antar.api.catalogue.web.dto;

import com.antar.api.catalogue.persistence.InsurerType;

import java.time.LocalDate;

public record InsurerResponse(
        Long id,
        String displayName,
        InsurerType insurerType,
        boolean isActive,
        /** Display name of the insurer this one merged into, if any. Null otherwise. */
        String cededTo,
        LocalDate lastVerifiedDate) {
}
