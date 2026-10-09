package com.th4.edge.decoboon.content.repository;

import com.th4.edge.decoboon.content.model.ContentTemplate;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** 内容模板仓储。 */
public interface ContentTemplateRepository extends JpaRepository<ContentTemplate, Long> {
    Optional<ContentTemplate> findByTemplateIdAndCompany_ComId(Long templateId, Long comId);
    Optional<ContentTemplate> findByTemplateIdAndCompanyIsNull(Long templateId);
    List<ContentTemplate> findByCompany_ComIdOrCompanyIsNullOrderByTemplateIdAsc(Long comId);
}
