package com.antar.engine;

import com.antar.engine.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GapEngineTest {

    private final GapEngine engine = new GapEngine();

    private Policy policyWithCoPay(String coPayPercent) {
        return new Policy(
                "Sample Insurer",
                "Family Floater 5L",
                Money.of("500000"),
                Money.of("500000"),
                new PolicyTerms(
                        RoomRentRule.percentOfSumInsured("0.01"),
                        List.of(SubLimit.of("CATARACT", "40000")),
                        CoPayRule.of(coPayPercent, CoPayOrder.AFTER_PROPORTION)));
    }

    private HospitalBill billWithRoomRate(String roomPerDay, String roomTotal) {
        return new HospitalBill("GENERAL_SURGERY", 5, Money.of(roomPerDay),
                List.of(
                        BillLine.of("Room charges", BillCategory.ROOM, roomTotal),
                        BillLine.of("Surgeon and anaesthetist", BillCategory.ASSOCIATED, "95000"),
                        BillLine.of("Nursing and OT", BillCategory.ASSOCIATED, "60000"),
                        BillLine.of("Investigations", BillCategory.ASSOCIATED, "35000"),
                        BillLine.of("Pharmacy", BillCategory.PHARMACY, "38000"),
                        BillLine.of("Implants", BillCategory.IMPLANT, "14000"),
                        BillLine.of("Gloves, admin, registration", BillCategory.NON_PAYABLE, "18000")));
    }

    @Test
    @DisplayName("Scenario 1: room within limit - no proportionate deduction")
    void roomWithinLimit_noProportionateDeduction() {
        Policy policy = policyWithCoPay("0");
        HospitalBill bill = billWithRoomRate("5000", "25000");

        GapResult result = engine.compute(bill, policy);

        // 25,000 room + 1,90,000 associated + 52,000 pharmacy/implants = 2,67,000
        assertThat(result.payout()).isEqualTo(Money.of("267000"));
        assertThat(result.gap()).isEqualTo(Money.of("18000"));
        assertThat(result.trace())
                .extracting(DeductionTrace::clauseReference)
                .containsExactly("NON_PAYABLE_ITEMS")
                .doesNotContain("ROOM_RENT_PROPORTION");
    }

    @Test
    @DisplayName("Scenario 2: room above limit - proportion hits associated charges only")
    void roomAboveLimit_proportionAppliedToAssociatedChargesOnly() {
        Policy policy = policyWithCoPay("0.10");
        HospitalBill bill = billWithRoomRate("8000", "40000");

        GapResult result = engine.compute(bill, policy);

        assertThat(result.totalBill()).isEqualTo(Money.of("300000"));
        assertThat(result.payout()).isEqualTo(Money.of("176175"));
        assertThat(result.gap()).isEqualTo(Money.of("123825"));
        assertThat(result.gapAsPercentOfBill()).isEqualByComparingTo("41.3");

        assertThat(result.trace())
                .extracting(DeductionTrace::clauseReference)
                .containsExactly("NON_PAYABLE_ITEMS", "ROOM_RENT_CAP",
                        "ROOM_RENT_PROPORTION", "CO_PAY");
    }

    @Test
    @DisplayName("Pharmacy and implants are NOT reduced by the room proportion")
    void pharmacyAndImplantsSurviveTheProportion() {
        Policy policy = policyWithCoPay("0");
        HospitalBill bill = billWithRoomRate("8000", "40000");

        GapResult result = engine.compute(bill, policy);

        // 25,000 room + (1,90,000 x 0.625) + 52,000 = 1,95,750
        assertThat(result.payout()).isEqualTo(Money.of("195750"));
    }

    @Test
    @DisplayName("Scenario 3: procedure sub-limit caps the payout")
    void subLimitCapsThePayout() {
        Policy policy = policyWithCoPay("0");
        HospitalBill bill = new HospitalBill("CATARACT", 1, Money.of("5000"),
                List.of(
                        BillLine.of("Room charges", BillCategory.ROOM, "5000"),
                        BillLine.of("Surgeon fee", BillCategory.ASSOCIATED, "45000"),
                        BillLine.of("Lens implant", BillCategory.IMPLANT, "30000")));

        GapResult result = engine.compute(bill, policy);

        assertThat(result.payout()).isEqualTo(Money.of("40000"));
        assertThat(result.gap()).isEqualTo(Money.of("40000"));
        assertThat(result.trace())
                .extracting(DeductionTrace::clauseReference)
                .contains("PROCEDURE_SUB_LIMIT");
    }

    @Test
    @DisplayName("Payout can never exceed the remaining sum insured")
    void payoutCappedByRemainingSumInsured() {
        Policy policy = new Policy("Sample Insurer", "Family Floater 5L",
                Money.of("500000"), Money.of("100000"),
                new PolicyTerms(RoomRentRule.percentOfSumInsured("0.01"),
                        List.of(), CoPayRule.NONE));
        HospitalBill bill = billWithRoomRate("5000", "25000");

        GapResult result = engine.compute(bill, policy);

        assertThat(result.payout()).isEqualTo(Money.of("100000"));
        assertThat(result.trace())
                .extracting(DeductionTrace::clauseReference)
                .contains("SUM_INSURED_EXHAUSTED");
    }

    @Test
    @DisplayName("Every deduction is traceable to a clause")
    void everyDeductionIsExplained() {
        GapResult result = engine.compute(billWithRoomRate("8000", "40000"), policyWithCoPay("0.10"));

        assertThat(result.trace()).allSatisfy(t -> {
            assertThat(t.clauseReference()).isNotBlank();
            assertThat(t.explanation()).isNotBlank();
        });
        assertThat(result.totalDeductions()).isEqualTo(result.gap());
    }
}
