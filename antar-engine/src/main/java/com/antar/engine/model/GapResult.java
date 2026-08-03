package com.antar.engine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** The answer: what the family pays, and exactly why. */
public record GapResult(
        Money totalBill,
        Money payout,
        Money gap,
        List<DeductionTrace> trace) {

    public GapResult {
        if (totalBill == null || payout == null || gap == null) {
            throw new IllegalArgumentException("amounts must not be null");
        }
        trace = trace == null ? List.of() : List.copyOf(trace);
    }

    /** The number people actually react to. */
    public BigDecimal gapAsPercentOfBill() {
        if (totalBill.isZero()) {
            return BigDecimal.ZERO;
        }
        return gap.amount()
                .divide(totalBill.amount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    public Money totalDeductions() {
        return trace.stream()
                .map(DeductionTrace::amountRemoved)
                .reduce(Money.ZERO, Money::plus);
    }
}
