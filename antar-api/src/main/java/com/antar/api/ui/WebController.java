package com.antar.api.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.antar.api.service.GapService;
import com.antar.api.web.dto.BillLineRequest;
import com.antar.api.web.dto.DeductionTraceResponse;
import com.antar.api.web.dto.GapComputeRequest;
import com.antar.api.web.dto.GapComputeResponse;
import com.antar.api.web.dto.PolicyRequest;
import com.antar.api.web.dto.PolicyResponse;
import com.antar.engine.model.BillCategory;

@Controller
public class WebController {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final GapService service;

    public WebController(GapService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String policyForm(Model model) {
        model.addAttribute("policyForm", new PolicyForm());
        return "policy";
    }

    @PostMapping("/policy")
    public String savePolicy(@ModelAttribute PolicyForm form, Model model) {
        PolicyRequest request = new PolicyRequest(
                blankTo(form.getInsurerName(), "My insurer"),
                blankTo(form.getProductName(), "My policy"),
                form.getSumInsured(),
                form.getSumInsured(),
                asFraction(form.getRoomRentPercent()),
                form.isProportionPharmacy(),
                asFraction(form.getCoPayPercent()),
                form.getCoPayOrder(),
                List.of());

        PolicyResponse saved = service.createPolicy(request);
        return "redirect:/bill/" + saved.id();
    }

    @GetMapping("/bill/{policyId}")
    public String billForm(@PathVariable Long policyId, Model model) {
        model.addAttribute("policy", service.findPolicy(policyId));
        model.addAttribute("billForm", new BillForm());
        model.addAttribute("policyId", policyId);
        return "bill";
    }

    @PostMapping("/bill/{policyId}")
    public String computeGap(@PathVariable Long policyId, @ModelAttribute BillForm form, Model model) {

        List<BillLineRequest> lines = new ArrayList<>();
        addLine(lines, "Room and bed charges", BillCategory.ROOM, form.getRoomTotal());
        addLine(lines, "Doctor, nursing, OT and investigations", BillCategory.ASSOCIATED, form.getAssociatedTotal());
        addLine(lines, "Pharmacy and consumables", BillCategory.PHARMACY, form.getPharmacyTotal());
        addLine(lines, "Implants and devices", BillCategory.IMPLANT, form.getImplantTotal());
        addLine(lines, "Non-payable items", BillCategory.NON_PAYABLE, form.getNonPayableTotal());

        GapComputeRequest request = new GapComputeRequest(
                policyId,
                blankTo(form.getProcedureCode(), "HOSPITALISATION"),
                form.getDaysAdmitted(),
                form.getActualRoomRentPerDay(),
                lines);

        GapComputeResponse result = service.computeGap(request);

        model.addAttribute("policy", service.findPolicy(policyId));
        model.addAttribute("result", result);
        model.addAttribute("policyId", policyId);
        model.addAttribute("runningBalance", runningBalance(result));
        return "result";
    }

    /** The balance remaining after each deduction, so the ledger can show the erosion. */
    private List<BigDecimal> runningBalance(GapComputeResponse result) {
        List<BigDecimal> balances = new ArrayList<>();
        BigDecimal balance = result.totalBill();
        for (DeductionTraceResponse deduction : result.deductions()) {
            balance = balance.subtract(deduction.amountRemoved());
            balances.add(balance);
        }
        return balances;
    }

    private void addLine(List<BillLineRequest> lines, String description, BillCategory category, BigDecimal amount) {
        if (amount != null && amount.signum() > 0) {
            lines.add(new BillLineRequest(description, category, amount));
        }
    }

    private BigDecimal asFraction(BigDecimal percent) {
        return percent.divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }

    private String blankTo(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static final Locale INDIA = Locale.forLanguageTag("en-IN");

    public static String rupees(BigDecimal amount) {
        NumberFormat format = NumberFormat.getInstance(INDIA);
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }
}
