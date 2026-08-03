package com.antar.engine;

import com.antar.engine.model.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Block C smoke test. Proves the domain model holds a real scenario.
 * No GapEngine yet - that is Block D. This only shows the model reads correctly.
 */
public class DomainModelDemo {

    public static void main(String[] args) {

        PolicyTerms terms = new PolicyTerms(
                RoomRentRule.percentOfSumInsured("0.01"),
                List.of(SubLimit.of("CATARACT", "40000")),
                CoPayRule.of("0.10", CoPayOrder.AFTER_PROPORTION));

        Policy policy = new Policy(
                "Sample Insurer",
                "Family Floater 5L",
                Money.of("500000"),
                Money.of("500000"),
                terms);

        HospitalBill bill = new HospitalBill(
                "GENERAL_SURGERY",
                5,
                Money.of("8000"),
                List.of(
                        BillLine.of("Room charges 5 days @ 8000", BillCategory.ROOM, "40000"),
                        BillLine.of("Surgeon and anaesthetist fees", BillCategory.ASSOCIATED, "95000"),
                        BillLine.of("Nursing and OT charges", BillCategory.ASSOCIATED, "60000"),
                        BillLine.of("Investigations", BillCategory.ASSOCIATED, "35000"),
                        BillLine.of("Pharmacy", BillCategory.PHARMACY, "38000"),
                        BillLine.of("Implants", BillCategory.IMPLANT, "14000"),
                        BillLine.of("Gloves, admin, registration", BillCategory.NON_PAYABLE, "18000")));

        Money eligibleRoom = policy.eligibleRoomRentPerDay();
        BigDecimal proportion = eligibleRoom.ratioTo(bill.actualRoomRentPerDay());

        System.out.println("=== ANTAR :: domain model smoke test ===");
        System.out.println();
        System.out.println("Insurer                 : " + policy.insurerName() + " / " + policy.productName());
        System.out.println("Sum insured             : " + policy.sumInsured());
        System.out.println("Eligible room rent/day  : " + eligibleRoom);
        System.out.println("Actual room rent/day    : " + bill.actualRoomRentPerDay());
        System.out.println("Room proportion factor  : " + proportion);
        System.out.println();
        System.out.println("Total bill              : " + bill.total());
        System.out.println("  ROOM                  : " + bill.amountIn(BillCategory.ROOM));
        System.out.println("  ASSOCIATED            : " + bill.amountIn(BillCategory.ASSOCIATED));
        System.out.println("  PHARMACY              : " + bill.amountIn(BillCategory.PHARMACY));
        System.out.println("  IMPLANT               : " + bill.amountIn(BillCategory.IMPLANT));
        System.out.println("  NON_PAYABLE           : " + bill.amountIn(BillCategory.NON_PAYABLE));
        System.out.println();
        System.out.println("Sub-limit for CATARACT  : " + terms.subLimitFor("CATARACT"));
        System.out.println("Sub-limit for this proc : " + terms.subLimitFor(bill.procedureCode()));
        System.out.println("Co-pay applies          : " + terms.coPayRule().applies()
                + "  (retained factor " + terms.coPayRule().retainedFactor() + ", order "
                + terms.coPayRule().order() + ")");
        System.out.println();
        System.out.println("Model holds. GapEngine.compute() is Block D.");
    }
}
