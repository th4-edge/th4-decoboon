package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.model.CarryoverPlanConfirmation;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 接管后施工计划确认仓储。 */
public interface CarryoverPlanConfirmationRepository extends JpaRepository<CarryoverPlanConfirmation, Long> {
    Optional<CarryoverPlanConfirmation> findFirstByItem_ItemIdAndPlanVersionOrderByConfirmationIdDesc(
            Long itemId, Integer planVersion);
}
