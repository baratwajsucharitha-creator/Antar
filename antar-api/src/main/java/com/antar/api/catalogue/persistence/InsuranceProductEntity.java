package com.antar.api.catalogue.persistence;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "insurance_product")
public class InsuranceProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "insurer_id", nullable = false)
    private Long insurerId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_category", nullable = false, length = 40)
    private ProductCategory productCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment", nullable = false, length = 20)
    private Segment segment;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.UNKNOWN;

    @Column(name = "first_cleared_date")
    private LocalDate firstClearedDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "source", nullable = false, length = 300)
    private String source;

    @Column(name = "last_verified_date", nullable = false)
    private LocalDate lastVerifiedDate;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    public InsuranceProductEntity() {
        // JPA requires a no-arg constructor
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.createdDate = now;
        this.updatedDate = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedDate = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public Long getInsurerId() { return insurerId; }
    public String getProductName() { return productName; }
    public ProductCategory getProductCategory() { return productCategory; }
    public Segment getSegment() { return segment; }
    public AvailabilityStatus getAvailabilityStatus() { return availabilityStatus; }
    public LocalDate getFirstClearedDate() { return firstClearedDate; }
    public String getNotes() { return notes; }
    public String getSource() { return source; }
    public LocalDate getLastVerifiedDate() { return lastVerifiedDate; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public LocalDateTime getUpdatedDate() { return updatedDate; }

    public void setInsurerId(Long v) { this.insurerId = v; }
    public void setProductName(String v) { this.productName = v; }
    public void setProductCategory(ProductCategory v) { this.productCategory = v; }
    public void setSegment(Segment v) { this.segment = v; }
    public void setAvailabilityStatus(AvailabilityStatus v) { this.availabilityStatus = v; }
    public void setFirstClearedDate(LocalDate v) { this.firstClearedDate = v; }
    public void setNotes(String v) { this.notes = v; }
    public void setSource(String v) { this.source = v; }
    public void setLastVerifiedDate(LocalDate v) { this.lastVerifiedDate = v; }
}
