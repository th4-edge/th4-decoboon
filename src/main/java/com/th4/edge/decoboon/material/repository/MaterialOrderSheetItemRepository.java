package com.th4.edge.decoboon.material.repository;

import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/** 主材下单作业单行仓储。 */
public interface MaterialOrderSheetItemRepository extends JpaRepository<MaterialOrderSheetItem, Long>,
        JpaSpecificationExecutor<MaterialOrderSheetItem> {

    @EntityGraph(attributePaths = {"materialPlan"})
    List<MaterialOrderSheetItem> findBySheet_SheetIdOrderBySortAscItemIdAsc(Long sheetId);

    @EntityGraph(attributePaths = {"materialPlan"})
    List<MaterialOrderSheetItem> findBySheet_SheetIdInOrderBySheet_SheetIdDescSortAscItemIdAsc(Collection<Long> sheetIds);

    @EntityGraph(attributePaths = {"materialPlan", "sheet"})
    List<MaterialOrderSheetItem> findByMaterialPlan_PlanId(Long planId);

    @EntityGraph(attributePaths = {"materialPlan", "sheet"})
    List<MaterialOrderSheetItem> findByMaterialPlan_PlanIdIn(Collection<Long> planIds);

    long countBySheet_SheetId(Long sheetId);

    void deleteBySheet_SheetId(Long sheetId);
}
