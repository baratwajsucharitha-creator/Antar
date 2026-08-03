package com.antar.engine.model;

/**
 * The most important type in this codebase.
 *
 * Every rupee the engine removes must produce one of these, naming the clause
 * that authorised the removal. If the engine cannot explain a deduction,
 * that is a bug, not a rounding difference.
 */
public record DeductionTrace(
        String clauseReference,
        String explanation,
        Money amountRemoved) {

    public DeductionTrace {
        if (clauseReference == null || clauseReference.isBlank()) {
            throw new IllegalArgumentException("every deduction must name a clause");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("every deduction must be explainable in words");
        }
        if (amountRemoved == null) {
            throw new IllegalArgumentException("amountRemoved must not be null");
        }
    }

    public static DeductionTrace of(String clauseReference, String explanation, Money amountRemoved) {
        return new DeductionTrace(clauseReference, explanation, amountRemoved);
    }
}
