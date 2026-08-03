package com.antar.engine.model;

/** A per-procedure cap, e.g. cataract capped at Rs.40,000. */
public record SubLimit(String procedureCode, Money cap) {

    public SubLimit {
        if (procedureCode == null || procedureCode.isBlank()) {
            throw new IllegalArgumentException("procedureCode must not be blank");
        }
        if (cap == null) {
            throw new IllegalArgumentException("cap must not be null");
        }
    }

    public static SubLimit of(String procedureCode, String cap) {
        return new SubLimit(procedureCode, Money.of(cap));
    }
}
