package com.th4.edge.decoboon.document.repository;

import com.th4.edge.decoboon.document.configure.DocumentTemplateVersionStatus;
import com.th4.edge.decoboon.document.model.DocumentTemplateVersion;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 企业单据模板版本仓储。 */
public interface DocumentTemplateVersionRepository extends JpaRepository<DocumentTemplateVersion, Long> {

    Optional<DocumentTemplateVersion> findByCompany_ComIdAndTemplate_TemplateIdAndVersion(
            Long comId, Long templateId, Integer version);

    Optional<DocumentTemplateVersion> findFirstByCompany_ComIdAndTemplate_TemplateIdAndStatusOrderByVersionDesc(
            Long comId, Long templateId, DocumentTemplateVersionStatus status);

    List<DocumentTemplateVersion> findByCompany_ComIdAndTemplate_TemplateIdOrderByVersionDesc(
            Long comId, Long templateId);

    List<DocumentTemplateVersion> findByCompany_ComIdAndTemplate_TemplateIdAndStatus(
            Long comId, Long templateId, DocumentTemplateVersionStatus status);
}
