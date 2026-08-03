package com.antar.engine.model;

import java.math.BigDecimal;

/**
 * @param percent 0.10 for a 10% co-pay
 * @param order   whether it applies before or after the room proportion
 */
public record CoPayRule(BigDecimal percent, CoPayOrder order) {

    public static final CoPayRule NONE = new CoPayRule(BigDecimal.ZERO, CoPayOrder.AFTER_PROPORTION);

    public CoPayRule {
        if (percent == null || percent.signum() < 0 || percent.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("percent must be between 0 and 1");
        }
        if (order == null) {
            throw new IllegalArgumentException("order must not be null");
        }
    }

    public static CoPayRule of(String percent, CoPayOrder order) {
        return new CoPayRule(new BigDecimal(percent), order);
    }

    public boolean applies() {
        return percent.signum() > 0;
    }

    /** The multiplier to apply, e.g. 0.90 for a 10% co-pay. */
    public BigDecimal retainedFactor() {
        return BigDecimal.ONE.subtract(percent);
    }
}
