package com.th4.edge.decoboon.pack.repository;

import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PackageLineRepository extends JpaRepository<PackageLine, Long> {

    java.util.Optional<PackageLine> findByItemIdAndCompany_ComId(Long itemId, Long comId);

    java.util.List<PackageLine> findByTemplate_TemplateIdOrderBySortAscItemIdAsc(Long templateId);

    java.util.List<PackageLine> findByTemplate_TemplateIdAndCompany_ComIdOrderBySortAscItemIdAsc(Long templateId, Long comId);

    java.util.Optional<PackageLine> findByTemplate_TemplateIdAndSection_GroupIdAndItem_ItemId(
            Long templateId, Long groupId, Long itemId);

    long countBySection_GroupId(Long groupId);

    @Query("""
            select line.template.templateId, count(line), count(distinct line.section.groupId)
            from PackageLine line
            where line.template.templateId in :templateIds
            group by line.template.templateId
            """)
    java.util.List<Object[]> summarizeByTemplateIds(@Param("templateIds") java.util.Collection<Long> templateIds);
}
