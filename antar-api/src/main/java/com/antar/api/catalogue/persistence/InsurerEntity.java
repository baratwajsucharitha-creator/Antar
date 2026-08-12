package com.antar.api.catalogue.persistence;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "insurer")
public class InsurerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "irdai_registration_no", nullable = false, length = 20)
    private String irdaiRegistrationNo;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "insurer_type", nullable = false, length = 30)
    private InsurerType insurerType;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "ceased_date")
    private LocalDate ceasedDate;

    @Column(name = "succeeded_by_insurer_id")
    private Long succeededByInsurerId;

    @Column(name = "source", nullable = false, length = 300)
    private String source;

    @Column(name = "last_verified_date", nullable = false)
    private LocalDate lastVerifiedDate;

    @Column(name = "created_date", nullable = false, updatable = false)
    private java.time.Instant createdDate;

    @Column(name = "updated_date", nullable = false)
    private java.time.Instant updatedDate;

    public InsurerEntity() {
        // JPA requires a no-arg constructor
    }

    @PrePersist
    void onCreate() {
        java.time.Instant now = java.time.Instant.now();
        this.createdDate = now;
        this.updatedDate = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedDate = java.time.Instant.now();
    }

    public Long getId() { return id; }
    public String getIrdaiRegistrationNo() { return irdaiRegistrationNo; }
    public String getLegalName() { return legalName; }
    public String getDisplayName() { return displayName; }
    public InsurerType getInsurerType() { return insurerType; }
    public boolean isActive() { return active; }
    public LocalDate getCeasedDate() { return ceasedDate; }
    public Long getSucceededByInsurerId() { return succeededByInsurerId; }
    public String getSource() { return source; }
    public LocalDate getLastVerifiedDate() { return lastVerifiedDate; }
    public java.time.Instant getCreatedDate() { return createdDate; }
    public java.time.Instant getUpdatedDate() { return updatedDate; }

    public void setIrdaiRegistrationNo(String v) { this.irdaiRegistrationNo = v; }
    public void setLegalName(String v) { this.legalName = v; }
    public void setDisplayName(String v) { this.displayName = v; }
    public void setInsurerType(InsurerType v) { this.insurerType = v; }
    public void setActive(boolean v) { this.active = v; }
    public void setCeasedDate(LocalDate v) { this.ceasedDate = v; }
    public void setSucceededByInsurerId(Long v) { this.succeededByInsurerId = v; }
    public void setSource(String v) { this.source = v; }
    public void setLastVerifiedDate(LocalDate v) { this.lastVerifiedDate = v; }
}
