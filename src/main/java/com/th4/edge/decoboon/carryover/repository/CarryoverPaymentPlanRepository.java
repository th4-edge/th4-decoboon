package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.model.CarryoverPaymentPlan;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 剩余收付款计划仓储。 */
public interface CarryoverPaymentPlanRepository extends JpaRepository<CarryoverPaymentPlan, Long>,
        JpaSpecificationExecutor<CarryoverPaymentPlan> {

    List<CarryoverPaymentPlan> findByCompany_ComIdAndItem_ItemIdOrderByDirectionAscSortAscPlanIdAsc(
            Long comId, Long itemId);

    void deleteByCompany_ComIdAndItem_ItemId(Long comId, Long itemId);
}
