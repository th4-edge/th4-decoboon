package com.th4.edge.decoboon.material.repository;

import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 主材下单单据模板仓储。 */
public interface MaterialSheetTemplateRepository extends JpaRepository<MaterialSheetTemplate, Long>,
        JpaSpecificationExecutor<MaterialSheetTemplate> {

    List<MaterialSheetTemplate> findByCompany_ComIdOrderBySheetTypeAscVersionDesc(Long comId);

    List<MaterialSheetTemplate> findByCompany_ComIdAndSheetTypeOrderByVersionDesc(Long comId, String sheetType);

    Optional<MaterialSheetTemplate> findFirstByCompany_ComIdAndSheetTypeAndPublishedVersionGreaterThanOrderByPublishedVersionDesc(
            Long comId, String sheetType, Integer publishedVersion);

    Optional<MaterialSheetTemplate> findFirstBySheetTemplateIdAndCompany_ComId(Long sheetTemplateId, Long comId);
}
