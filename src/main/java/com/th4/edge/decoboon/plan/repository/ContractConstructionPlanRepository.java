package com.th4.edge.decoboon.plan.repository;

import com.th4.edge.decoboon.plan.model.ContractConstructionPlan;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractConstructionPlanRepository extends JpaRepository<ContractConstructionPlan, Long> {
    Optional<ContractConstructionPlan> findByContract_ContractIdAndCompany_ComId(Long contractId, Long comId);
}
