package com.antar.api.catalogue.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsurerRepository extends JpaRepository<InsurerEntity, Long> {

    Optional<InsurerEntity> findByIrdaiRegistrationNo(String irdaiRegistrationNo);

    List<InsurerEntity> findByInsurerTypeOrderByDisplayName(InsurerType insurerType);

    List<InsurerEntity> findAllByOrderByDisplayName();

    List<InsurerEntity> findByDisplayNameContainingIgnoreCaseOrderByDisplayName(String query);

    List<InsurerEntity> findByInsurerTypeAndDisplayNameContainingIgnoreCaseOrderByDisplayName(
            InsurerType insurerType, String query);
}
