package com.th4.edge.decoboon.plan.repository;

import com.th4.edge.decoboon.plan.configure.PlanTemplateStatus;
import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 企业施工计划模板仓储。 */
public interface CompanyConstructionPlanRepository extends JpaRepository<CompanyConstructionPlan, Long>,
        JpaSpecificationExecutor<CompanyConstructionPlan> {

    List<CompanyConstructionPlan> findByCompany_ComIdOrderByDefaultPlanDescSortAscTemplateIdAsc(Long comId);

    List<CompanyConstructionPlan> findByCompany_ComIdAndStatusOrderByDefaultPlanDescSortAscTemplateIdAsc(
            Long comId, PlanTemplateStatus status);

    Optional<CompanyConstructionPlan> findByCompany_ComIdAndTemplateId(Long comId, Long templateId);

    Optional<CompanyConstructionPlan> findFirstByCompany_ComIdAndStatusOrderByDefaultPlanDescSortAscTemplateIdAsc(
            Long comId, PlanTemplateStatus status);

    Optional<CompanyConstructionPlan> findByCompany_ComIdAndCodeAndStatus(
            Long comId, String code, PlanTemplateStatus status);

    List<CompanyConstructionPlan> findByCompany_ComIdOrderByCodeAscVersionDescTemplateIdDesc(Long comId);

    Optional<CompanyConstructionPlan> findFirstByCompany_ComIdAndCodeOrderByVersionDesc(Long comId, String code);

    List<CompanyConstructionPlan> findByCompany_ComIdAndCodeAndCurrentEnabledTrue(Long comId, String code);

    List<CompanyConstructionPlan> findByCompany_ComIdAndCurrentEnabledTrue(Long comId);

    List<CompanyConstructionPlan> findByCompany_ComIdAndStatusAndCurrentEnabledTrueOrderByDefaultPlanDescTemplateIdAsc(
            Long comId, PlanTemplateStatus status);
}
