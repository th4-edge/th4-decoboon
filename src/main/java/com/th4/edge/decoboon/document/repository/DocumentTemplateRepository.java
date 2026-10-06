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

    Optional<DocumentTemplate> findFirstByCompany_ComIdAndTypeAndPresetCodeAndStatusOrderByVersionDesc(
            Long comId, DocumentType type, String presetCode, DocumentStatus status);

    Optional<DocumentTemplate> findByCompany_ComIdAndTemplateId(Long comId, Long templateId);

    List<DocumentTemplate> findByCompany_ComIdAndStatusOrderByTypeAscVersionDesc(Long comId, DocumentStatus status);

    List<DocumentTemplate> findByCompany_ComIdAndStatusOrderByTypeAscPresetCodeAscVersionDesc(
            Long comId, DocumentStatus status);
}
