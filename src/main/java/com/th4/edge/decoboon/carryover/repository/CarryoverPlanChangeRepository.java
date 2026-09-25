package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.model.CarryoverPlanChange;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 接管后施工计划调整记录仓储。 */
public interface CarryoverPlanChangeRepository extends JpaRepository<CarryoverPlanChange, Long> {
    List<CarryoverPlanChange> findByItem_ItemIdOrderByChangeIdDesc(Long itemId);

    @Modifying
    @Query("update CarryoverPlanChange change set change.stage = null where change.stage.stageId = :stageId")
    int detachStage(@Param("stageId") Long stageId);
}
