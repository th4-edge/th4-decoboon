package com.th4.edge.decoboon.plan.repository;

import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractPaymentNodeRepository extends JpaRepository<ContractPaymentNode, Long> {
    List<ContractPaymentNode> findByPlan_PlanIdOrderBySortAsc(Long planId);

    void deleteByPlan_PlanId(Long planId);
}
