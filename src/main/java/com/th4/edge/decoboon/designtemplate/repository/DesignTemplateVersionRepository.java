package com.th4.edge.decoboon.designtemplate.repository;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignTemplateVersionRepository extends JpaRepository<DesignTemplateVersion, Long> {

    Optional<DesignTemplateVersion> findByVersionIdAndCompany_ComId(Long versionId, Long comId);

    Optional<DesignTemplateVersion> findByTemplate_TemplateIdAndCompany_ComIdAndStatus(
            Long templateId, Long comId, DesignTemplate.Status status);

    List<DesignTemplateVersion> findByTemplate_TemplateIdAndCompany_ComIdOrderByVersionDesc(Long templateId, Long comId);
}
