package com.antar.api.catalogue.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVersionRepository extends JpaRepository<ProductVersionEntity, Long> {

    Optional<ProductVersionEntity> findByUin(String uin);

    List<ProductVersionEntity> findByProductIdOrderByEffectiveFromDesc(Long productId);
}
