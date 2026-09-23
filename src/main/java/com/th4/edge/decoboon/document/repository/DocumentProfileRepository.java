package com.th4.edge.decoboon.document.repository;

import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentProfileRepository extends JpaRepository<DocumentProfile, Long> {
    Optional<DocumentProfile> findByCompany_ComId(Long comId);
}
