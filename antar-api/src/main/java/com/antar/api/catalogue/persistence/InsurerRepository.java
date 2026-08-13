package com.antar.api.catalogue.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsurerRepository extends JpaRepository<InsurerEntity, Long> {

    Optional<InsurerEntity> findByIrdaiRegistrationNo(String irdaiRegistrationNo);

    /** Exact-match lookup used as the import natural key when no registration number is known. */
    Optional<InsurerEntity> findByDisplayName(String displayName);

    List<InsurerEntity> findByInsurerTypeOrderByDisplayName(InsurerType insurerType);

    List<InsurerEntity> findAllByOrderByDisplayName();

    List<InsurerEntity> findByActiveTrueOrderByDisplayName();

    List<InsurerEntity> findByDisplayNameContainingIgnoreCaseOrderByDisplayName(String query);

    List<InsurerEntity> findByInsurerTypeAndDisplayNameContainingIgnoreCaseOrderByDisplayName(
            InsurerType insurerType, String query);
}
