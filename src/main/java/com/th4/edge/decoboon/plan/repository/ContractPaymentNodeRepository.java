package com.th4.edge.decoboon.plan.repository;

import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractPaymentNodeRepository extends JpaRepository<ContractPaymentNode, Long> {
    List<ContractPaymentNode> findByPlan_PlanIdOrderBySortAsc(Long planId);

    void deleteByPlan_PlanId(Long planId);

    /** 合同工作台：按应收集合一次取回付款节点，替代 Receivable 反向 OneToOne 的逐条加载。 */
    java.util.List<ContractPaymentNode> findByReceivable_ReceivableIdIn(java.util.Collection<Long> receivableIds);
}
