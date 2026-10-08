package com.th4.edge.decoboon.designtemplate.repository;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DesignTemplateItemRepository extends JpaRepository<DesignTemplateItem, Long> {

    List<DesignTemplateItem> findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(Long versionId, Long comId);
}
