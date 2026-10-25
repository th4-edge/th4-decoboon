package com.th4.edge.decoboon.plan.repository;

import com.th4.edge.decoboon.plan.configure.PlanTemplateStatus;
import com.th4.edge.decoboon.plan.model.CompanyPaymentPlan;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 企业付款计划模板仓储。 */
public interface CompanyPaymentPlanRepository extends JpaRepository<CompanyPaymentPlan, Long>,
        JpaSpecificationExecutor<CompanyPaymentPlan> {

    List<CompanyPaymentPlan> findByCompany_ComIdOrderByDefaultPlanDescSortAscTemplateIdAsc(Long comId);

    List<CompanyPaymentPlan> findByCompany_ComIdAndStatusOrderByDefaultPlanDescSortAscTemplateIdAsc(
            Long comId, PlanTemplateStatus status);

    Optional<CompanyPaymentPlan> findByCompany_ComIdAndTemplateId(Long comId, Long templateId);

    Optional<CompanyPaymentPlan> findFirstByCompany_ComIdAndStatusOrderByDefaultPlanDescSortAscTemplateIdAsc(
            Long comId, PlanTemplateStatus status);

    Optional<CompanyPaymentPlan> findByCompany_ComIdAndCodeAndStatus(
            Long comId, String code, PlanTemplateStatus status);

    List<CompanyPaymentPlan> findByCompany_ComIdOrderByCodeAscVersionDescTemplateIdDesc(Long comId);

    Optional<CompanyPaymentPlan> findFirstByCompany_ComIdAndCodeOrderByVersionDesc(Long comId, String code);

    List<CompanyPaymentPlan> findByCompany_ComIdAndCodeAndCurrentEnabledTrue(Long comId, String code);

    List<CompanyPaymentPlan> findByCompany_ComIdAndCurrentEnabledTrue(Long comId);

    List<CompanyPaymentPlan> findByCompany_ComIdAndStatusAndCurrentEnabledTrueOrderByDefaultPlanDescTemplateIdAsc(
            Long comId, PlanTemplateStatus status);
}
