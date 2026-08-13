package com.antar.api.catalogue.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVersionRepository extends JpaRepository<ProductVersionEntity, Long> {

    Optional<ProductVersionEntity> findByUin(String uin);

    List<ProductVersionEntity> findByProductIdOrderByEffectiveFromDesc(Long productId);

    /**
     * One query for a whole batch of products, so a product-list response can compute
     * per-product version stats (count, current UIN) without firing one query per product.
     */
    List<ProductVersionEntity> findByProductIdInOrderByProductIdAscEffectiveFromDesc(List<Long> productIds);
}
