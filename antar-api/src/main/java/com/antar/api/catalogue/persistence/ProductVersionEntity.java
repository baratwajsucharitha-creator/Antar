package com.antar.api.catalogue.persistence;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "product_version")
public class ProductVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "uin", nullable = false, length = 50)
    private String uin;

    @Column(name = "version_label", length = 20)
    private String versionLabel;

    @Column(name = "irdai_cleared_date")
    private LocalDate irdaiClearedDate;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "wording_pdf_url", length = 500)
    private String wordingPdfUrl;

    @Column(name = "source", nullable = false, length = 300)
    private String source;

    @Column(name = "last_verified_date", nullable = false)
    private LocalDate lastVerifiedDate;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    public ProductVersionEntity() {
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
    public Long getProductId() { return productId; }
    public String getUin() { return uin; }
    public String getVersionLabel() { return versionLabel; }
    public LocalDate getIrdaiClearedDate() { return irdaiClearedDate; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getSourceUrl() { return sourceUrl; }
    public String getWordingPdfUrl() { return wordingPdfUrl; }
    public String getSource() { return source; }
    public LocalDate getLastVerifiedDate() { return lastVerifiedDate; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public LocalDateTime getUpdatedDate() { return updatedDate; }

    public void setProductId(Long v) { this.productId = v; }
    public void setUin(String v) { this.uin = v; }
    public void setVersionLabel(String v) { this.versionLabel = v; }
    public void setIrdaiClearedDate(LocalDate v) { this.irdaiClearedDate = v; }
    public void setEffectiveFrom(LocalDate v) { this.effectiveFrom = v; }
    public void setEffectiveTo(LocalDate v) { this.effectiveTo = v; }
    public void setVerificationStatus(VerificationStatus v) { this.verificationStatus = v; }
    public void setSourceUrl(String v) { this.sourceUrl = v; }
    public void setWordingPdfUrl(String v) { this.wordingPdfUrl = v; }
    public void setSource(String v) { this.source = v; }
    public void setLastVerifiedDate(LocalDate v) { this.lastVerifiedDate = v; }
}
