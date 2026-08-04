package com.antar.api.ui;

import com.antar.engine.model.CoPayOrder;

import java.math.BigDecimal;

/** Mutable form backing bean. Thymeleaf form binding needs setters, so this is not a record. */
public class PolicyForm {

    private String insurerName = "";
    private String productName = "";
    private BigDecimal sumInsured = new BigDecimal("500000");
    private BigDecimal roomRentPercent = new BigDecimal("1.00");
    private boolean proportionPharmacy = false;
    private BigDecimal coPayPercent = new BigDecimal("0");
    private CoPayOrder coPayOrder = CoPayOrder.AFTER_PROPORTION;

    public String getInsurerName() { return insurerName; }
    public void setInsurerName(String v) { this.insurerName = v; }

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public BigDecimal getSumInsured() { return sumInsured; }
    public void setSumInsured(BigDecimal v) { this.sumInsured = v; }

    /** Entered as a whole number of percent, e.g. 1.00 for "1% of sum insured per day". */
    public BigDecimal getRoomRentPercent() { return roomRentPercent; }
    public void setRoomRentPercent(BigDecimal v) { this.roomRentPercent = v; }

    public boolean isProportionPharmacy() { return proportionPharmacy; }
    public void setProportionPharmacy(boolean v) { this.proportionPharmacy = v; }

    public BigDecimal getCoPayPercent() { return coPayPercent; }
    public void setCoPayPercent(BigDecimal v) { this.coPayPercent = v; }

    public CoPayOrder getCoPayOrder() { return coPayOrder; }
    public void setCoPayOrder(CoPayOrder v) { this.coPayOrder = v; }
}
