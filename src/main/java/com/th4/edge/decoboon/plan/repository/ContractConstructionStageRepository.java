package com.th4.edge.decoboon.plan.repository;

import com.th4.edge.decoboon.plan.model.ContractConstructionStage;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractConstructionStageRepository extends JpaRepository<ContractConstructionStage, Long> {
    List<ContractConstructionStage> findByPlan_PlanIdOrderBySortAsc(Long planId);

    void deleteByPlan_PlanId(Long planId);
}
