package com.antar.api.ui;

import java.math.BigDecimal;

/**
 * The UI collects category totals rather than every line item.
 * Families read a hospital bill in blocks, not line by line - and the engine
 * only ever cares about the category. The API still accepts full itemisation.
 */
public class BillForm {

    private String procedureCode = "GENERAL_SURGERY";
    private int daysAdmitted = 5;
    private BigDecimal actualRoomRentPerDay = new BigDecimal("8000");
    private BigDecimal roomTotal = new BigDecimal("40000");
    private BigDecimal associatedTotal = new BigDecimal("190000");
    private BigDecimal pharmacyTotal = new BigDecimal("38000");
    private BigDecimal implantTotal = new BigDecimal("14000");
    private BigDecimal nonPayableTotal = new BigDecimal("18000");

    public String getProcedureCode() { return procedureCode; }
    public void setProcedureCode(String v) { this.procedureCode = v; }

    public int getDaysAdmitted() { return daysAdmitted; }
    public void setDaysAdmitted(int v) { this.daysAdmitted = v; }

    public BigDecimal getActualRoomRentPerDay() { return actualRoomRentPerDay; }
    public void setActualRoomRentPerDay(BigDecimal v) { this.actualRoomRentPerDay = v; }

    public BigDecimal getRoomTotal() { return roomTotal; }
    public void setRoomTotal(BigDecimal v) { this.roomTotal = v; }

    public BigDecimal getAssociatedTotal() { return associatedTotal; }
    public void setAssociatedTotal(BigDecimal v) { this.associatedTotal = v; }

    public BigDecimal getPharmacyTotal() { return pharmacyTotal; }
    public void setPharmacyTotal(BigDecimal v) { this.pharmacyTotal = v; }

    public BigDecimal getImplantTotal() { return implantTotal; }
    public void setImplantTotal(BigDecimal v) { this.implantTotal = v; }

    public BigDecimal getNonPayableTotal() { return nonPayableTotal; }
    public void setNonPayableTotal(BigDecimal v) { this.nonPayableTotal = v; }
}
