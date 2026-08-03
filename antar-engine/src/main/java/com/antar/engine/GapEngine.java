package com.antar.engine;

import com.antar.engine.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The deterministic core of Antar.
 *
 * No framework, no randomness, no LLM. Given a bill and a policy, the same
 * inputs always produce the same rupee figure, and every deduction applied
 * emits a DeductionTrace naming the clause that authorised it.
 */
public class GapEngine {

    public GapResult compute(HospitalBill bill, Policy policy) {

        List<DeductionTrace> trace = new ArrayList<>();
        PolicyTerms terms = policy.terms();

        // ---- Step 1: strip non-payables (IRDAI Annexure I) ----------------
        Money nonPayable = bill.amountIn(BillCategory.NON_PAYABLE);
        if (!nonPayable.isZero()) {
            trace.add(DeductionTrace.of(
                    "NON_PAYABLE_ITEMS",
                    "Consumables and administrative items excluded under IRDAI Annexure I",
                    nonPayable));
        }

        // ---- Step 2: cap the room at the eligible rent --------------------
        Money eligiblePerDay = policy.eligibleRoomRentPerDay();
        Money actualPerDay = bill.actualRoomRentPerDay();
        Money claimedRoom = bill.amountIn(BillCategory.ROOM);

        boolean roomExceedsLimit = actualPerDay.isGreaterThan(eligiblePerDay);
        Money allowedRoom = roomExceedsLimit
                ? eligiblePerDay.times(bill.daysAdmitted())
                : claimedRoom;

        if (roomExceedsLimit) {
            trace.add(DeductionTrace.of(
                    "ROOM_RENT_CAP",
                    "Room taken at " + actualPerDay + "/day against an eligible "
                            + eligiblePerDay + "/day; excess room charge borne by the insured",
                    claimedRoom.minus(allowedRoom)));
        }

        // ---- Step 3: proportionate deduction on associated charges --------
        BigDecimal proportion = roomExceedsLimit
                ? eligiblePerDay.ratioTo(actualPerDay)
                : BigDecimal.ONE;

        Money associated = bill.amountIn(BillCategory.ASSOCIATED);
        Money allowedAssociated = associated.multiply(proportion);

        if (roomExceedsLimit) {
            trace.add(DeductionTrace.of(
                    "ROOM_RENT_PROPORTION",
                    "Associated charges reduced by factor " + proportion
                            + " because the room category exceeded the eligible limit",
                    associated.minus(allowedAssociated)));
        }

        // Pharmacy and implants: proportioned only if the policy says so.
        Money pharmacy = bill.amountIn(BillCategory.PHARMACY);
        Money implants = bill.amountIn(BillCategory.IMPLANT);
        Money nonProportioned = pharmacy.plus(implants);

        if (roomExceedsLimit && terms.roomRentRule().proportionPharmacy()) {
            Money reduced = nonProportioned.multiply(proportion);
            trace.add(DeductionTrace.of(
                    "ROOM_RENT_PROPORTION_PHARMACY",
                    "This insurer also applies the room proportion to pharmacy and implants",
                    nonProportioned.minus(reduced)));
            nonProportioned = reduced;
        }

        Money payable = allowedRoom.plus(allowedAssociated).plus(nonProportioned);

        // ---- Step 4: co-pay, in the order this insurer applies it ---------
        CoPayRule coPay = terms.coPayRule();
        if (coPay.applies()) {
            Money afterCoPay = payable.multiply(coPay.retainedFactor());
            trace.add(DeductionTrace.of(
                    "CO_PAY",
                    "Co-payment of " + coPay.percent().multiply(BigDecimal.valueOf(100))
                            + "% applied " + coPay.order(),
                    payable.minus(afterCoPay)));
            payable = afterCoPay;
        }

        // ---- Step 5: procedure sub-limit ----------------------------------
        Optional<SubLimit> subLimit = terms.subLimitFor(bill.procedureCode());
        if (subLimit.isPresent() && payable.isGreaterThan(subLimit.get().cap())) {
            Money cap = subLimit.get().cap();
            trace.add(DeductionTrace.of(
                    "PROCEDURE_SUB_LIMIT",
                    "Procedure " + bill.procedureCode() + " capped at " + cap
                            + " under the policy sub-limit schedule",
                    payable.minus(cap)));
            payable = cap;
        }

        // ---- Step 6: remaining sum insured --------------------------------
        Money remaining = policy.remainingSumInsured();
        if (payable.isGreaterThan(remaining)) {
            trace.add(DeductionTrace.of(
                    "SUM_INSURED_EXHAUSTED",
                    "Payout limited to the remaining sum insured of " + remaining,
                    payable.minus(remaining)));
            payable = remaining;
        }

        Money total = bill.total();
        return new GapResult(total, payable, total.minus(payable), trace);
    }
}
