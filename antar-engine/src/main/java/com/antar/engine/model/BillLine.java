package com.antar.engine.model;

/** One line item on a hospital bill. */
public record BillLine(String description, BillCategory category, Money amount) {

    public BillLine {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
    }

    public static BillLine of(String description, BillCategory category, String amount) {
        return new BillLine(description, category, Money.of(amount));
    }
}
