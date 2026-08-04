package com.antar.api.persistence;

import com.antar.engine.model.CoPayOrder;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "policy")
public class PolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "insurer_name", nullable = false, length = 120)
    private String insurerName;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(name = "sum_insured", nullable = false, precision = 15, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "remaining_sum_insured", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingSumInsured;

    @Column(name = "room_rent_percent", nullable = false, precision = 6, scale = 4)
    private BigDecimal roomRentPercentOfSumInsuredPerDay;

    @Column(name = "proportion_pharmacy", nullable = false)
    private boolean proportionPharmacy;

    @Column(name = "co_pay_percent", nullable = false, precision = 5, scale = 4)
    private BigDecimal coPayPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "co_pay_order", nullable = false, length = 30)
    private CoPayOrder coPayOrder;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "policy_sub_limit", joinColumns = @JoinColumn(name = "policy_id"))
    private List<SubLimitEmbeddable> subLimits = new ArrayList<>();

    public PolicyEntity() {
        // JPA requires a no-arg constructor; the mapper also uses it
    }

    public Long getId() { return id; }
    public String getInsurerName() { return insurerName; }
    public String getProductName() { return productName; }
    public BigDecimal getSumInsured() { return sumInsured; }
    public BigDecimal getRemainingSumInsured() { return remainingSumInsured; }
    public BigDecimal getRoomRentPercentOfSumInsuredPerDay() { return roomRentPercentOfSumInsuredPerDay; }
    public boolean isProportionPharmacy() { return proportionPharmacy; }
    public BigDecimal getCoPayPercent() { return coPayPercent; }
    public CoPayOrder getCoPayOrder() { return coPayOrder; }
    public List<SubLimitEmbeddable> getSubLimits() { return subLimits; }

    public void setInsurerName(String v) { this.insurerName = v; }
    public void setProductName(String v) { this.productName = v; }
    public void setSumInsured(BigDecimal v) { this.sumInsured = v; }
    public void setRemainingSumInsured(BigDecimal v) { this.remainingSumInsured = v; }
    public void setRoomRentPercentOfSumInsuredPerDay(BigDecimal v) { this.roomRentPercentOfSumInsuredPerDay = v; }
    public void setProportionPharmacy(boolean v) { this.proportionPharmacy = v; }
    public void setCoPayPercent(BigDecimal v) { this.coPayPercent = v; }
    public void setCoPayOrder(CoPayOrder v) { this.coPayOrder = v; }
    public void setSubLimits(List<SubLimitEmbeddable> v) { this.subLimits = v; }
}
