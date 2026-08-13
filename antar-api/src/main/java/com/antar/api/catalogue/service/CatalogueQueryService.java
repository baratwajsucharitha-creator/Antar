package com.antar.api.catalogue.service;

import com.antar.api.catalogue.persistence.*;
import com.antar.api.catalogue.web.CatalogueEntityNotFoundException;
import com.antar.api.catalogue.web.dto.InsurerResponse;
import com.antar.api.catalogue.web.dto.ProductSummaryResponse;
import com.antar.api.catalogue.web.dto.ProductVersionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only queries over the public catalogue (insurer / insurance_product / product_version).
 * Deliberately not owner-scoped - this is reference data, not a caller's own records.
 */
@Service
@Transactional(readOnly = true)
public class CatalogueQueryService {

    private final InsurerRepository insurerRepository;
    private final InsuranceProductRepository productRepository;
    private final ProductVersionRepository versionRepository;

    public CatalogueQueryService(InsurerRepository insurerRepository,
                                  InsuranceProductRepository productRepository,
                                  ProductVersionRepository versionRepository) {
        this.insurerRepository = insurerRepository;
        this.productRepository = productRepository;
        this.versionRepository = versionRepository;
    }

    public List<InsurerResponse> listInsurers(InsurerType type, String q) {
        List<InsurerEntity> insurers = selectInsurers(type, q);

        Set<Long> successorIds = insurers.stream()
                .map(InsurerEntity::getSucceededByInsurerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> successorNames = successorIds.isEmpty()
                ? Map.of()
                : insurerRepository.findAllById(successorIds).stream()
                        .collect(Collectors.toMap(InsurerEntity::getId, InsurerEntity::getDisplayName));

        return insurers.stream()
                .map(insurer -> new InsurerResponse(
                        insurer.getId(),
                        insurer.getDisplayName(),
                        insurer.getInsurerType(),
                        insurer.isActive(),
                        insurer.getSucceededByInsurerId() == null
                                ? null
                                : successorNames.get(insurer.getSucceededByInsurerId()),
                        insurer.getLastVerifiedDate()))
                .toList();
    }

    private List<InsurerEntity> selectInsurers(InsurerType type, String q) {
        boolean hasType = type != null;
        boolean hasQuery = q != null && !q.isBlank();
        if (hasType && hasQuery) {
            return insurerRepository.findByInsurerTypeAndDisplayNameContainingIgnoreCaseOrderByDisplayName(type, q);
        }
        if (hasType) {
            return insurerRepository.findByInsurerTypeOrderByDisplayName(type);
        }
        if (hasQuery) {
            return insurerRepository.findByDisplayNameContainingIgnoreCaseOrderByDisplayName(q);
        }
        return insurerRepository.findAllByOrderByDisplayName();
    }

    /**
     * Unfiltered by default - a policyholder's own product may be withdrawn, and that
     * is still the correct answer for them. Filtering by availability is opt-in.
     *
     * Version stats (count, current UIN) are computed from one bulk query over every
     * matched product's versions, not one query per product - see the repository method
     * this calls.
     */
    public List<ProductSummaryResponse> listProducts(Long insurerId, AvailabilityStatus availability, String q) {
        if (!insurerRepository.existsById(insurerId)) {
            throw new CatalogueEntityNotFoundException("insurer", insurerId);
        }

        List<InsuranceProductEntity> products = availability == null
                ? productRepository.findByInsurerIdOrderByProductName(insurerId)
                : productRepository.findByInsurerIdAndAvailabilityStatusOrderByProductName(insurerId, availability);

        if (q != null && !q.isBlank()) {
            String needle = q.toLowerCase(Locale.ROOT);
            products = products.stream()
                    .filter(p -> p.getProductName().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }

        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream().map(InsuranceProductEntity::getId).toList();
        Map<Long, List<ProductVersionEntity>> versionsByProduct =
                versionRepository.findByProductIdInOrderByProductIdAscEffectiveFromDesc(productIds).stream()
                        .collect(Collectors.groupingBy(ProductVersionEntity::getProductId));

        return products.stream()
                .map(product -> {
                    List<ProductVersionEntity> versions = versionsByProduct.getOrDefault(product.getId(), List.of());
                    String currentUin = versions.stream()
                            .filter(v -> v.getEffectiveTo() == null)
                            .map(ProductVersionEntity::getUin)
                            .findFirst()
                            .orElse(null);
                    return new ProductSummaryResponse(
                            product.getId(),
                            product.getProductName(),
                            product.getProductCategory(),
                            product.getSegment(),
                            product.getAvailabilityStatus(),
                            versions.size(),
                            currentUin);
                })
                .toList();
    }

    /**
     * Without asOf, every version is returned, oldest last. With asOf, only versions
     * whose [effectiveFrom, effectiveTo] window contains that date - an open (null)
     * effectiveTo means "still current". Zero matches (e.g. asOf before any version
     * existed) is a valid, honest answer, not an error.
     */
    public List<ProductVersionResponse> listVersions(Long productId, LocalDate asOf) {
        if (!productRepository.existsById(productId)) {
            throw new CatalogueEntityNotFoundException("product", productId);
        }

        List<ProductVersionEntity> versions = versionRepository.findByProductIdOrderByEffectiveFromDesc(productId);

        if (asOf != null) {
            versions = versions.stream().filter(v -> wasInForce(v, asOf)).toList();
        }

        return versions.stream()
                .map(v -> new ProductVersionResponse(
                        v.getId(), v.getUin(), v.getVersionLabel(), v.getEffectiveFrom(), v.getEffectiveTo(),
                        v.getVerificationStatus(), v.getWordingPdfUrl()))
                .toList();
    }

    private boolean wasInForce(ProductVersionEntity version, LocalDate asOf) {
        LocalDate from = version.getEffectiveFrom();
        LocalDate to = version.getEffectiveTo();
        boolean startedByThen = from == null || !from.isAfter(asOf);
        boolean notYetEnded = to == null || !to.isBefore(asOf);
        return startedByThen && notYetEnded;
    }
}
