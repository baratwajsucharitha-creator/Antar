package com.antar.api.catalogue.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProductEntity, Long> {

    Optional<InsuranceProductEntity> findByInsurerIdAndProductNameAndSegment(
            Long insurerId, String productName, Segment segment);

    /** Unfiltered by default - a policyholder's product may be withdrawn, and that is still correct data. */
    List<InsuranceProductEntity> findByInsurerIdOrderByProductName(Long insurerId);

    List<InsuranceProductEntity> findByInsurerIdAndAvailabilityStatusOrderByProductName(
            Long insurerId, AvailabilityStatus availabilityStatus);
}
