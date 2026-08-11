package com.antar.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<PolicyEntity, Long> {

    /**
     * Ownership is part of the lookup, not a check performed afterwards.
     * A policy belonging to someone else is not "found and rejected" - it is
     * simply not found, so the API cannot be used to probe which ids exist.
     */
    Optional<PolicyEntity> findByIdAndOwnerId(Long id, String ownerId);

    List<PolicyEntity> findAllByOwnerIdOrderByIdDesc(String ownerId);
}
