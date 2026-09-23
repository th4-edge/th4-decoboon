package com.th4.edge.decoboon.plan.repository;

import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractPaymentPlanRepository extends JpaRepository<ContractPaymentPlan, Long> {
    Optional<ContractPaymentPlan> findByContract_ContractIdAndCompany_ComId(Long contractId, Long comId);
}
