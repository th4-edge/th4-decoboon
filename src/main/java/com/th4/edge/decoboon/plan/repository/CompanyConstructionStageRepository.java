package com.th4.edge.decoboon.plan.repository;

import com.th4.edge.decoboon.plan.model.CompanyConstructionStage;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 企业施工计划阶段仓储。 */
public interface CompanyConstructionStageRepository extends JpaRepository<CompanyConstructionStage, Long> {

    List<CompanyConstructionStage> findByPlan_TemplateIdOrderBySortAscStageIdAsc(Long templateId);

    void deleteByPlan_TemplateId(Long templateId);
}
