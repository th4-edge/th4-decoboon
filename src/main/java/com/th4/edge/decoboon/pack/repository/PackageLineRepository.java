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

    @Query(value = """
            select line.section_id,
                   coalesce(line.catalog_type_snapshot, catalog.type) as item_type,
                   count(*),
                   sum(case
                       when line.pricing_method = 'FixedTotal'
                           then coalesce(catalog.fixed_total_price, 0)
                       when line.pricing_method = 'MaterialOnly'
                           then coalesce(catalog.material_price, 0) * line.quantity_factor
                       when line.pricing_method = 'ManualOnly'
                           then coalesce(catalog.manual_price, 0) * line.quantity_factor
                       else (coalesce(catalog.material_price, 0) + coalesce(catalog.manual_price, 0)) * line.quantity_factor
                   end) as reference_amount,
                   sum(coalesce(line.cost_price_snapshot, catalog.cost_price, 0) * line.quantity_factor) as reference_cost
            from pack_line line
            join catalog_item catalog on catalog.item_id = line.catalog_item_id
            where line.template_id = :templateId
            group by line.section_id, coalesce(line.catalog_type_snapshot, catalog.type)
            """, nativeQuery = true)
    java.util.List<Object[]> summarizeTypeCountsByTemplateId(@Param("templateId") Long templateId);
}
