package com.antar.api.ui;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formats whole-rupee amounts with Indian digit grouping (last 3 digits together,
 * then groups of 2: 300000 -> 3,00,000). Java's built-in NumberFormat for the
 * en-IN locale does not actually implement this grouping on this JDK - it groups
 * by 3 like en-US, verified directly - so this is a hand-rolled formatter rather
 * than a Locale lookup.
 */
@Component
public class RupeeFormatter {

    public String format(BigDecimal amount) {
        BigDecimal whole = amount.setScale(0, RoundingMode.HALF_UP);
        String sign = whole.signum() < 0 ? "-" : "";
        String digits = whole.abs().toBigInteger().toString();
        return sign + "₹" + group(digits);
    }

    private String group(String digits) {
        if (digits.length() <= 3) {
            return digits;
        }
        String lastThree = digits.substring(digits.length() - 3);
        String rest = digits.substring(0, digits.length() - 3);

        StringBuilder grouped = new StringBuilder();
        int i = rest.length();
        while (i > 2) {
            grouped.insert(0, "," + rest.substring(i - 2, i));
            i -= 2;
        }
        grouped.insert(0, rest.substring(0, i));

        return grouped + "," + lastThree;
    }
}
