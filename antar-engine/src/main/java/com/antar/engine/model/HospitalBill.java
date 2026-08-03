package com.antar.engine.model;

import java.util.List;

/** A single hospitalisation event, as the hospital billed it. */
public record HospitalBill(
        String procedureCode,
        int daysAdmitted,
        Money actualRoomRentPerDay,
        List<BillLine> lines) {

    public HospitalBill {
        if (daysAdmitted < 1) {
            throw new IllegalArgumentException("daysAdmitted must be at least 1");
        }
        if (actualRoomRentPerDay == null) {
            throw new IllegalArgumentException("actualRoomRentPerDay must not be null");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("bill must have at least one line");
        }
        lines = List.copyOf(lines);
    }

    /** Everything the hospital asked for, including non-payables. */
    public Money total() {
        return lines.stream()
                .map(BillLine::amount)
                .reduce(Money.ZERO, Money::plus);
    }

    public Money amountIn(BillCategory category) {
        return lines.stream()
                .filter(line -> line.category() == category)
                .map(BillLine::amount)
                .reduce(Money.ZERO, Money::plus);
    }

    public List<BillLine> linesIn(BillCategory category) {
        return lines.stream().filter(line -> line.category() == category).toList();
    }
}
