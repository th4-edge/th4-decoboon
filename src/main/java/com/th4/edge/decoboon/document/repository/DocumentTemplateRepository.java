package com.th4.edge.decoboon.document.repository;

import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {
    Optional<DocumentTemplate> findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(
            Long comId, DocumentType type, DocumentStatus status);

    List<DocumentTemplate> findByCompany_ComIdAndStatusOrderByTypeAscVersionDesc(Long comId, DocumentStatus status);
}
