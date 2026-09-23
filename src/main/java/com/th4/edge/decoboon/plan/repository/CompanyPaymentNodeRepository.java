package com.th4.edge.decoboon.plan.repository;

import com.th4.edge.decoboon.plan.model.CompanyPaymentNode;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 企业付款计划节点仓储。 */
public interface CompanyPaymentNodeRepository extends JpaRepository<CompanyPaymentNode, Long> {

    List<CompanyPaymentNode> findByPlan_TemplateIdOrderBySortAscNodeIdAsc(Long templateId);

    void deleteByPlan_TemplateId(Long templateId);
}
