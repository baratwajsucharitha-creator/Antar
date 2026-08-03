package com.antar.engine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A rupee amount. Always BigDecimal, always scale 2, always HALF_UP.
 * No double ever enters the calculation path.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final Money ZERO = Money.of("0");

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public static Money of(long value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public Money plus(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    /** Multiply by a ratio or percentage factor, e.g. the room proportion 0.625. */
    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor));
    }

    public Money times(int count) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(count)));
    }

    public Money min(Money other) {
        return this.compareTo(other) <= 0 ? this : other;
    }

    public boolean isGreaterThan(Money other) {
        return this.compareTo(other) > 0;
    }

    public boolean isZero() {
        return this.amount.signum() == 0;
    }

    /** Ratio of this to other, scale 6, for proportion factors. Never rounded to 2. */
    public BigDecimal ratioTo(Money other) {
        if (other.isZero()) {
            throw new ArithmeticException("cannot compute ratio against zero");
        }
        return this.amount.divide(other.amount, 6, ROUNDING);
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return "Rs." + amount.toPlainString();
    }
}
