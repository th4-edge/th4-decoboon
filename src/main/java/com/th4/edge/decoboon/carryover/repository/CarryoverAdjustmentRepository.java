package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.model.CarryoverAdjustment;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 承接调整记录仓储。 */
public interface CarryoverAdjustmentRepository extends JpaRepository<CarryoverAdjustment, Long>,
        JpaSpecificationExecutor<CarryoverAdjustment> {

    List<CarryoverAdjustment> findByItem_ItemIdOrderByAdjustmentIdDesc(Long itemId);
}
