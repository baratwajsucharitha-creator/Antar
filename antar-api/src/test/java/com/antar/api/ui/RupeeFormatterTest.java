package com.antar.api.ui;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RupeeFormatterTest {

    private final RupeeFormatter formatter = new RupeeFormatter();

    @Test
    void groupsSixDigitsAsThreeThenTwoThenThree() {
        assertThat(formatter.format(new BigDecimal("300000"))).isEqualTo("₹3,00,000");
        assertThat(formatter.format(new BigDecimal("123825"))).isEqualTo("₹1,23,825");
        assertThat(formatter.format(new BigDecimal("500000"))).isEqualTo("₹5,00,000");
    }

    @Test
    void groupsFiveDigits() {
        assertThat(formatter.format(new BigDecimal("18000"))).isEqualTo("₹18,000");
    }

    @Test
    void groupsFourDigits() {
        assertThat(formatter.format(new BigDecimal("1000"))).isEqualTo("₹1,000");
    }

    @Test
    void underAThousandHasNoGrouping() {
        assertThat(formatter.format(new BigDecimal("999"))).isEqualTo("₹999");
        assertThat(formatter.format(BigDecimal.ZERO)).isEqualTo("₹0");
    }

    @Test
    void roundsToWholeRupees() {
        assertThat(formatter.format(new BigDecimal("176175.49"))).isEqualTo("₹1,76,175");
        assertThat(formatter.format(new BigDecimal("176175.50"))).isEqualTo("₹1,76,176");
    }

    @Test
    void groupsLargeCroreScaleAmounts() {
        assertThat(formatter.format(new BigDecimal("12345678"))).isEqualTo("₹1,23,45,678");
    }
}
