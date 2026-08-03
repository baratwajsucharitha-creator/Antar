package com.antar.engine.model;

/**
 * Where the co-pay sits in the deduction pipeline.
 * Insurers genuinely differ here, and the order changes the final number.
 * This is the reason rule ordering is modelled as data, not hardcoded.
 */
public enum CoPayOrder {
    BEFORE_PROPORTION,
    AFTER_PROPORTION
}
