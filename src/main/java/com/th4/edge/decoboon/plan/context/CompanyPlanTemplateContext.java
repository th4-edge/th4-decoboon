package com.th4.edge.decoboon.plan.context;

import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.plan.configure.PaymentTriggerType;
import com.th4.edge.decoboon.plan.configure.PlanTemplateSource;
import com.th4.edge.decoboon.plan.configure.PlanTemplateStatus;
import com.th4.edge.decoboon.plan.configure.StageAcceptanceMode;
import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.decoboon.plan.model.CompanyConstructionStage;
import com.th4.edge.decoboon.plan.model.CompanyPaymentNode;
import com.th4.edge.decoboon.plan.model.CompanyPaymentPlan;
import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.ConstructionStageTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanNodeTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.decoboon.plan.model.PlanTemplateCatalog;
import com.th4.edge.decoboon.plan.repository.CompanyConstructionPlanRepository;
import com.th4.edge.decoboon.plan.repository.CompanyConstructionStageRepository;
import com.th4.edge.decoboon.plan.repository.CompanyPaymentNodeRepository;
import com.th4.edge.decoboon.plan.repository.CompanyPaymentPlanRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** 企业自定义付款与施工计划模板维护。 */
@Component
public class CompanyPlanTemplateContext {

    private final TenantContext tenantContext;
    private final PlanTemplateBaselineContext baselineContext;
    private final CompanyPaymentPlanRepository paymentPlanRepository;
    private final CompanyPaymentNodeRepository paymentNodeRepository;
    private final CompanyConstructionPlanRepository constructionPlanRepository;
    private final CompanyConstructionStageRepository constructionStageRepository;

    public CompanyPlanTemplateContext(TenantContext tenantContext,
                                      PlanTemplateBaselineContext baselineContext,
                                      CompanyPaymentPlanRepository paymentPlanRepository,
                                      CompanyPaymentNodeRepository paymentNodeRepository,
                                      CompanyConstructionPlanRepository constructionPlanRepository,
                                      CompanyConstructionStageRepository constructionStageRepository) {
        this.tenantContext = tenantContext;
        this.baselineContext = baselineContext;
        this.paymentPlanRepository = paymentPlanRepository;
        this.paymentNodeRepository = paymentNodeRepository;
        this.constructionPlanRepository = constructionPlanRepository;
        this.constructionStageRepository = constructionStageRepository;
    }

    @Transactional(readOnly = true)
    public PlanTemplateCatalog availableCatalog(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        return catalog(current, false);
    }

    @Transactional(readOnly = true)
    public PlanTemplateCatalog managementCatalog(Employee principal) {
        // 所有内部岗位均可进入模板管理页；非负责人/店长仅由 Provider 写权限和后端维护动作阻止修改。
        Employee current = tenantContext.requireEmployee(principal);
        return catalog(current, true);
    }

    @Transactional(readOnly = true)
    public PaymentPlanTemplate paymentInfo(Employee principal, CompanyPaymentPlan request) {
        Employee current = tenantContext.requireEmployee(principal);
        CompanyPaymentPlan plan = requirePaymentPlan(current, request == null ? null : request.getTemplateId());
        return paymentView(plan);
    }

    @Transactional(readOnly = true)
    public ConstructionPlanTemplate constructionInfo(Employee principal, CompanyConstructionPlan request) {
        Employee current = tenantContext.requireEmployee(principal);
        CompanyConstructionPlan plan = requireConstructionPlan(current, request == null ? null : request.getTemplateId());
        return constructionView(plan);
    }

    /** 企业负责人、店长可维护付款与施工计划模板；其他内部岗位仅可查看。 */
    private Employee requireMaintainer(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.StoreManager);
    }

    @Transactional
    public PaymentPlanTemplate savePayment(Employee principal, CompanyPaymentPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyPaymentPlan plan = new CompanyPaymentPlan();
        plan.setCompany(current.getCompany());
        plan.setCode("PAY_CUSTOM_" + token());
        plan.setVersion(1);
        plan.setStatus(PlanTemplateStatus.Draft);
        plan.setDefaultPlan(Boolean.FALSE);
        List<CompanyPaymentNode> nodes;
        if (hasText(request.getSourceBaselineCode())) {
            PaymentPlanTemplate baseline = baselineContext.paymentPlan(request.getSourceBaselineCode());
            plan.setName(hasText(request.getName()) ? request.getName() : baseline.getName() + "（企业版）");
            plan.setDescription(hasText(request.getDescription()) ? request.getDescription() : baseline.getDescription());
            plan.setSort(request.getSort() == null ? baseline.getSort() : request.getSort());
            plan.setSourceBaselineCode(baseline.getCode());
            plan.setSourceBaselineVersion(baseline.getVersion());
            nodes = paymentNodesFromBaseline(plan, baseline.getNodes());
        } else {
            applyPayment(plan, request);
            nodes = paymentNodes(plan, request.getNodes());
        }
        validatePayment(plan, nodes);
        plan = paymentPlanRepository.save(plan);
        replacePaymentNodes(plan, nodes);
        return paymentView(plan);
    }

    @Transactional
    public PaymentPlanTemplate updatePayment(Employee principal, CompanyPaymentPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyPaymentPlan plan = requirePaymentPlan(current, request == null ? null : request.getTemplateId());
        List<CompanyPaymentNode> nodes = paymentNodes(plan, request.getNodes());
        applyPayment(plan, request);
        if (plan.getStatus() == PlanTemplateStatus.Enabled) {
            plan.setVersion(Math.max(1, value(plan.getVersion())) + 1);
            plan.setPublishedAt(LocalDateTime.now());
        }
        validatePayment(plan, nodes);
        plan = paymentPlanRepository.save(plan);
        replacePaymentNodes(plan, nodes);
        return paymentView(plan);
    }

    @Transactional
    public PaymentPlanTemplate publishPayment(Employee principal, CompanyPaymentPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyPaymentPlan plan = requirePaymentPlan(current, request == null ? null : request.getTemplateId());
        List<CompanyPaymentNode> nodes = paymentNodeRepository.findByPlan_TemplateIdOrderBySortAscNodeIdAsc(plan.getTemplateId());
        validatePayment(plan, nodes);
        plan.setStatus(PlanTemplateStatus.Enabled);
        plan.setPublishedAt(LocalDateTime.now());
        if (plan.getVersion() == null || plan.getVersion() < 1) plan.setVersion(1);
        if (!hasPaymentDefault(current)) plan.setDefaultPlan(Boolean.TRUE);
        paymentPlanRepository.save(plan);
        return paymentView(plan);
    }

    @Transactional
    public PaymentPlanTemplate disablePayment(Employee principal, CompanyPaymentPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyPaymentPlan plan = requirePaymentPlan(current, request == null ? null : request.getTemplateId());
        plan.setStatus(PlanTemplateStatus.Disabled);
        plan.setDefaultPlan(Boolean.FALSE);
        paymentPlanRepository.save(plan);
        return paymentView(plan);
    }

    @Transactional
    public PaymentPlanTemplate defaultPayment(Employee principal, CompanyPaymentPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyPaymentPlan plan = requirePaymentPlan(current, request == null ? null : request.getTemplateId());
        requireEnabled(plan.getStatus(), "只有已启用的付款计划模板可以设为默认");
        for (CompanyPaymentPlan item : paymentPlanRepository.findByCompany_ComIdOrderByDefaultPlanDescSortAscTemplateIdAsc(current.getCompany().getComId())) {
            item.setDefaultPlan(item.getTemplateId().equals(plan.getTemplateId()));
            paymentPlanRepository.save(item);
        }
        return paymentView(plan);
    }

    @Transactional
    public ConstructionPlanTemplate saveConstruction(Employee principal, CompanyConstructionPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyConstructionPlan plan = new CompanyConstructionPlan();
        plan.setCompany(current.getCompany());
        plan.setCode("CON_CUSTOM_" + token());
        plan.setVersion(1);
        plan.setStatus(PlanTemplateStatus.Draft);
        plan.setDefaultPlan(Boolean.FALSE);
        List<CompanyConstructionStage> stages;
        if (hasText(request.getSourceBaselineCode())) {
            ConstructionPlanTemplate baseline = baselineContext.constructionPlan(request.getSourceBaselineCode());
            plan.setName(hasText(request.getName()) ? request.getName() : baseline.getName() + "（企业版）");
            plan.setDescription(hasText(request.getDescription()) ? request.getDescription() : baseline.getDescription());
            plan.setSort(request.getSort() == null ? baseline.getSort() : request.getSort());
            plan.setTotalDays(baseline.getTotalDays());
            plan.setSourceBaselineCode(baseline.getCode());
            plan.setSourceBaselineVersion(baseline.getVersion());
            stages = constructionStagesFromBaseline(plan, baseline.getStages());
        } else {
            applyConstruction(plan, request);
            stages = constructionStages(plan, request.getStages());
        }
        validateConstruction(plan, stages);
        plan.setTotalDays(stages.stream().mapToInt(item -> Math.max(1, item.getDefaultDays())).sum());
        plan = constructionPlanRepository.save(plan);
        replaceConstructionStages(plan, stages);
        return constructionView(plan);
    }

    @Transactional
    public ConstructionPlanTemplate updateConstruction(Employee principal, CompanyConstructionPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyConstructionPlan plan = requireConstructionPlan(current, request == null ? null : request.getTemplateId());
        List<CompanyConstructionStage> stages = constructionStages(plan, request.getStages());
        applyConstruction(plan, request);
        plan.setTotalDays(stages.stream().mapToInt(item -> Math.max(1, item.getDefaultDays())).sum());
        if (plan.getStatus() == PlanTemplateStatus.Enabled) {
            plan.setVersion(Math.max(1, value(plan.getVersion())) + 1);
            plan.setPublishedAt(LocalDateTime.now());
        }
        validateConstruction(plan, stages);
        plan = constructionPlanRepository.save(plan);
        replaceConstructionStages(plan, stages);
        return constructionView(plan);
    }

    @Transactional
    public ConstructionPlanTemplate publishConstruction(Employee principal, CompanyConstructionPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyConstructionPlan plan = requireConstructionPlan(current, request == null ? null : request.getTemplateId());
        List<CompanyConstructionStage> stages = constructionStageRepository.findByPlan_TemplateIdOrderBySortAscStageIdAsc(plan.getTemplateId());
        validateConstruction(plan, stages);
        plan.setTotalDays(stages.stream().mapToInt(item -> Math.max(1, item.getDefaultDays())).sum());
        plan.setStatus(PlanTemplateStatus.Enabled);
        plan.setPublishedAt(LocalDateTime.now());
        if (plan.getVersion() == null || plan.getVersion() < 1) plan.setVersion(1);
        if (!hasConstructionDefault(current)) plan.setDefaultPlan(Boolean.TRUE);
        constructionPlanRepository.save(plan);
        return constructionView(plan);
    }

    @Transactional
    public ConstructionPlanTemplate disableConstruction(Employee principal, CompanyConstructionPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyConstructionPlan plan = requireConstructionPlan(current, request == null ? null : request.getTemplateId());
        plan.setStatus(PlanTemplateStatus.Disabled);
        plan.setDefaultPlan(Boolean.FALSE);
        constructionPlanRepository.save(plan);
        return constructionView(plan);
    }

    @Transactional
    public ConstructionPlanTemplate defaultConstruction(Employee principal, CompanyConstructionPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyConstructionPlan plan = requireConstructionPlan(current, request == null ? null : request.getTemplateId());
        requireEnabled(plan.getStatus(), "只有已启用的施工计划模板可以设为默认");
        for (CompanyConstructionPlan item : constructionPlanRepository.findByCompany_ComIdOrderByDefaultPlanDescSortAscTemplateIdAsc(current.getCompany().getComId())) {
            item.setDefaultPlan(item.getTemplateId().equals(plan.getTemplateId()));
            constructionPlanRepository.save(item);
        }
        return constructionView(plan);
    }

    @Transactional(readOnly = true)
    public PaymentPlanTemplate resolvePaymentPlan(Long comId, String code) {
        CompanyPaymentPlan custom = hasText(code)
                ? paymentPlanRepository.findByCompany_ComIdAndCodeAndStatus(comId, code, PlanTemplateStatus.Enabled).orElse(null)
                : paymentPlanRepository.findFirstByCompany_ComIdAndStatusOrderByDefaultPlanDescSortAscTemplateIdAsc(
                        comId, PlanTemplateStatus.Enabled).orElse(null);
        if (custom != null) return paymentView(custom);
        return baselineContext.paymentPlan(code);
    }

    @Transactional(readOnly = true)
    public ConstructionPlanTemplate resolveConstructionPlan(Long comId, String code) {
        CompanyConstructionPlan custom = hasText(code)
                ? constructionPlanRepository.findByCompany_ComIdAndCodeAndStatus(comId, code, PlanTemplateStatus.Enabled).orElse(null)
                : constructionPlanRepository.findFirstByCompany_ComIdAndStatusOrderByDefaultPlanDescSortAscTemplateIdAsc(
                        comId, PlanTemplateStatus.Enabled).orElse(null);
        if (custom != null) return constructionView(custom);
        return baselineContext.constructionPlan(code);
    }

    private PlanTemplateCatalog catalog(Employee current, boolean includeDisabled) {
        Long comId = current.getCompany().getComId();
        List<PaymentPlanTemplate> payments = new ArrayList<>();
        List<CompanyPaymentPlan> customPayments = includeDisabled
                ? paymentPlanRepository.findByCompany_ComIdOrderByDefaultPlanDescSortAscTemplateIdAsc(comId)
                : paymentPlanRepository.findByCompany_ComIdAndStatusOrderByDefaultPlanDescSortAscTemplateIdAsc(
                        comId, PlanTemplateStatus.Enabled);
        customPayments.forEach(item -> payments.add(paymentView(item)));
        baselineContext.catalog().getPaymentPlans().forEach(item -> payments.add(baselinePayment(item)));

        List<ConstructionPlanTemplate> constructions = new ArrayList<>();
        List<CompanyConstructionPlan> customConstructions = includeDisabled
                ? constructionPlanRepository.findByCompany_ComIdOrderByDefaultPlanDescSortAscTemplateIdAsc(comId)
                : constructionPlanRepository.findByCompany_ComIdAndStatusOrderByDefaultPlanDescSortAscTemplateIdAsc(
                        comId, PlanTemplateStatus.Enabled);
        customConstructions.forEach(item -> constructions.add(constructionView(item)));
        baselineContext.catalog().getConstructionPlans().forEach(item -> constructions.add(baselineConstruction(item)));

        PlanTemplateCatalog result = new PlanTemplateCatalog();
        result.setPaymentPlanBaselineVersion(baselineContext.catalog().getPaymentPlanBaselineVersion());
        result.setConstructionPlanBaselineVersion(baselineContext.catalog().getConstructionPlanBaselineVersion());
        result.setPaymentPlans(payments.stream().sorted(templateComparator(PaymentPlanTemplate::getSource, PaymentPlanTemplate::getDefaultPlan, PaymentPlanTemplate::getSort)).toList());
        result.setConstructionPlans(constructions.stream().sorted(templateComparator(ConstructionPlanTemplate::getSource, ConstructionPlanTemplate::getDefaultPlan, ConstructionPlanTemplate::getSort)).toList());
        return result;
    }

    private <T> Comparator<T> templateComparator(java.util.function.Function<T, PlanTemplateSource> source,
                                                  java.util.function.Function<T, Boolean> defaultPlan,
                                                  java.util.function.Function<T, Integer> sort) {
        return Comparator.comparingInt((T item) -> source.apply(item) == PlanTemplateSource.Company ? 0 : 1)
                .thenComparingInt(item -> Boolean.TRUE.equals(defaultPlan.apply(item)) ? 0 : 1)
                .thenComparingInt(item -> sort.apply(item) == null ? 0 : sort.apply(item));
    }

    private PaymentPlanTemplate baselinePayment(PaymentPlanTemplate baseline) {
        baseline.setSource(PlanTemplateSource.Baseline);
        baseline.setStatus(PlanTemplateStatus.Enabled);
        baseline.setDefaultPlan(Boolean.FALSE);
        return baseline;
    }

    private ConstructionPlanTemplate baselineConstruction(ConstructionPlanTemplate baseline) {
        baseline.setSource(PlanTemplateSource.Baseline);
        baseline.setStatus(PlanTemplateStatus.Enabled);
        baseline.setDefaultPlan(Boolean.FALSE);
        return baseline;
    }

    private PaymentPlanTemplate paymentView(CompanyPaymentPlan plan) {
        PaymentPlanTemplate view = new PaymentPlanTemplate();
        view.setTemplateId(plan.getTemplateId());
        view.setCode(plan.getCode());
        view.setVersion(plan.getVersion());
        view.setName(plan.getName());
        view.setDescription(plan.getDescription());
        view.setSort(plan.getSort());
        view.setSource(PlanTemplateSource.Company);
        view.setStatus(plan.getStatus());
        view.setDefaultPlan(Boolean.TRUE.equals(plan.getDefaultPlan()));
        view.setNodes(paymentNodeRepository.findByPlan_TemplateIdOrderBySortAscNodeIdAsc(plan.getTemplateId()).stream()
                .map(this::paymentNodeView).toList());
        return view;
    }

    private ConstructionPlanTemplate constructionView(CompanyConstructionPlan plan) {
        ConstructionPlanTemplate view = new ConstructionPlanTemplate();
        view.setTemplateId(plan.getTemplateId());
        view.setCode(plan.getCode());
        view.setVersion(plan.getVersion());
        view.setName(plan.getName());
        view.setDescription(plan.getDescription());
        view.setTotalDays(plan.getTotalDays());
        view.setSort(plan.getSort());
        view.setSource(PlanTemplateSource.Company);
        view.setStatus(plan.getStatus());
        view.setDefaultPlan(Boolean.TRUE.equals(plan.getDefaultPlan()));
        view.setStages(constructionStageRepository.findByPlan_TemplateIdOrderBySortAscStageIdAsc(plan.getTemplateId()).stream()
                .map(this::constructionStageView).toList());
        return view;
    }

    private PaymentPlanNodeTemplate paymentNodeView(CompanyPaymentNode node) {
        PaymentPlanNodeTemplate view = new PaymentPlanNodeTemplate();
        view.setCode(node.getCode());
        view.setName(node.getName());
        view.setRatio(node.getRatio());
        view.setType(node.getType());
        view.setTriggerType(node.getTriggerType());
        view.setTriggerStageCode(node.getTriggerStageCode());
        view.setOffsetDays(node.getOffsetDays());
        view.setSort(node.getSort());
        return view;
    }

    private ConstructionStageTemplate constructionStageView(CompanyConstructionStage stage) {
        ConstructionStageTemplate view = new ConstructionStageTemplate();
        view.setCode(stage.getCode());
        view.setName(stage.getName());
        view.setDefaultDays(stage.getDefaultDays());
        view.setAcceptanceMode(stage.getAcceptanceMode());
        view.setSort(stage.getSort());
        view.setDescription(stage.getDescription());
        return view;
    }

    private List<CompanyPaymentNode> paymentNodesFromBaseline(CompanyPaymentPlan plan,
                                                              List<PaymentPlanNodeTemplate> templates) {
        List<CompanyPaymentNode> result = new ArrayList<>();
        for (int index = 0; index < templates.size(); index++) {
            PaymentPlanNodeTemplate item = templates.get(index);
            CompanyPaymentNode node = new CompanyPaymentNode();
            node.setCompany(plan.getCompany());
            node.setPlan(plan);
            node.setCode(item.getCode());
            node.setName(item.getName());
            node.setRatio(item.getRatio());
            node.setType(item.getType());
            node.setTriggerType(item.getTriggerType());
            node.setTriggerStageCode(item.getTriggerStageCode());
            node.setOffsetDays(item.getOffsetDays());
            node.setSort(item.getSort() == null ? index + 1 : item.getSort());
            result.add(node);
        }
        return result;
    }

    private List<CompanyPaymentNode> paymentNodes(CompanyPaymentPlan plan, List<CompanyPaymentNode> requestNodes) {
        List<CompanyPaymentNode> result = new ArrayList<>();
        List<CompanyPaymentNode> source = requestNodes == null ? List.of() : requestNodes;
        for (int index = 0; index < source.size(); index++) {
            CompanyPaymentNode item = source.get(index);
            CompanyPaymentNode node = new CompanyPaymentNode();
            node.setCompany(plan.getCompany());
            node.setPlan(plan);
            node.setCode(text(item.getCode()));
            node.setName(text(item.getName()));
            node.setRatio(item.getRatio());
            node.setType(item.getType() == null ? ReceivableType.Progress : item.getType());
            node.setTriggerType(item.getTriggerType() == null ? PaymentTriggerType.ContractSigned : item.getTriggerType());
            node.setTriggerStageCode(textOrNull(item.getTriggerStageCode()));
            node.setOffsetDays(item.getOffsetDays() == null ? 0 : item.getOffsetDays());
            node.setSort(item.getSort() == null ? index + 1 : item.getSort());
            result.add(node);
        }
        return result;
    }

    private List<CompanyConstructionStage> constructionStagesFromBaseline(CompanyConstructionPlan plan,
                                                                          List<ConstructionStageTemplate> templates) {
        List<CompanyConstructionStage> result = new ArrayList<>();
        for (int index = 0; index < templates.size(); index++) {
            ConstructionStageTemplate item = templates.get(index);
            CompanyConstructionStage stage = new CompanyConstructionStage();
            stage.setCompany(plan.getCompany());
            stage.setPlan(plan);
            stage.setCode(item.getCode());
            stage.setName(item.getName());
            stage.setDefaultDays(item.getDefaultDays());
            stage.setAcceptanceMode(item.getAcceptanceMode());
            stage.setSort(item.getSort() == null ? index + 1 : item.getSort());
            stage.setDescription(item.getDescription());
            result.add(stage);
        }
        return result;
    }

    private List<CompanyConstructionStage> constructionStages(CompanyConstructionPlan plan,
                                                              List<CompanyConstructionStage> requestStages) {
        List<CompanyConstructionStage> result = new ArrayList<>();
        List<CompanyConstructionStage> source = requestStages == null ? List.of() : requestStages;
        for (int index = 0; index < source.size(); index++) {
            CompanyConstructionStage item = source.get(index);
            CompanyConstructionStage stage = new CompanyConstructionStage();
            stage.setCompany(plan.getCompany());
            stage.setPlan(plan);
            stage.setCode(text(item.getCode()));
            stage.setName(text(item.getName()));
            stage.setDefaultDays(item.getDefaultDays() == null ? 1 : item.getDefaultDays());
            stage.setAcceptanceMode(item.getAcceptanceMode() == null ? StageAcceptanceMode.None : item.getAcceptanceMode());
            stage.setSort(item.getSort() == null ? index + 1 : item.getSort());
            stage.setDescription(textOrNull(item.getDescription()));
            result.add(stage);
        }
        return result;
    }

    private void replacePaymentNodes(CompanyPaymentPlan plan, List<CompanyPaymentNode> nodes) {
        paymentNodeRepository.deleteByPlan_TemplateId(plan.getTemplateId());
        for (CompanyPaymentNode node : nodes) {
            node.setPlan(plan);
            node.setCompany(plan.getCompany());
            paymentNodeRepository.save(node);
        }
    }

    private void replaceConstructionStages(CompanyConstructionPlan plan, List<CompanyConstructionStage> stages) {
        constructionStageRepository.deleteByPlan_TemplateId(plan.getTemplateId());
        for (CompanyConstructionStage stage : stages) {
            stage.setPlan(plan);
            stage.setCompany(plan.getCompany());
            constructionStageRepository.save(stage);
        }
    }

    private void applyPayment(CompanyPaymentPlan plan, CompanyPaymentPlan request) {
        if (!hasText(request.getName())) throw invalid("付款计划模板名称不能为空");
        plan.setName(request.getName().trim());
        plan.setDescription(textOrNull(request.getDescription()));
        plan.setSort(request.getSort() == null ? 0 : request.getSort());
    }

    private void applyConstruction(CompanyConstructionPlan plan, CompanyConstructionPlan request) {
        if (!hasText(request.getName())) throw invalid("施工计划模板名称不能为空");
        plan.setName(request.getName().trim());
        plan.setDescription(textOrNull(request.getDescription()));
        plan.setSort(request.getSort() == null ? 0 : request.getSort());
    }

    private void validatePayment(CompanyPaymentPlan plan, List<CompanyPaymentNode> nodes) {
        if (!hasText(plan.getName())) throw invalid("付款计划模板名称不能为空");
        if (nodes == null || nodes.isEmpty()) throw invalid("付款计划至少需要一个收款节点");
        if (nodes.size() > 10) throw invalid("付款计划最多支持十个收款节点");
        long unique = nodes.stream().map(CompanyPaymentNode::getCode).filter(this::hasText).distinct().count();
        if (unique != nodes.size()) throw invalid("付款计划节点编码不能为空或重复");
        BigDecimal total = BigDecimal.ZERO;
        for (CompanyPaymentNode node : nodes) {
            if (!hasText(node.getName())) throw invalid("付款节点名称不能为空");
            if (node.getRatio() == null || node.getRatio().signum() <= 0
                    || node.getRatio().compareTo(BigDecimal.ONE) > 0) {
                throw invalid("付款节点比例必须大于0且不超过100%");
            }
            if (requiresStage(node.getTriggerType()) && !hasText(node.getTriggerStageCode())) {
                throw invalid("阶段触发的付款节点必须选择施工阶段");
            }
            total = total.add(node.getRatio());
        }
        if (total.compareTo(BigDecimal.ONE) != 0) throw invalid("付款计划比例合计必须为100%");
    }

    private void validateConstruction(CompanyConstructionPlan plan, List<CompanyConstructionStage> stages) {
        if (!hasText(plan.getName())) throw invalid("施工计划模板名称不能为空");
        if (stages == null || stages.isEmpty()) throw invalid("施工计划至少需要一个施工阶段");
        if (stages.size() > 20) throw invalid("施工计划最多支持二十个阶段");
        long unique = stages.stream().map(CompanyConstructionStage::getCode).filter(this::hasText).distinct().count();
        if (unique != stages.size()) throw invalid("施工阶段编码不能为空或重复");
        for (CompanyConstructionStage stage : stages) {
            if (!hasText(stage.getName())) throw invalid("施工阶段名称不能为空");
            if (stage.getDefaultDays() == null || stage.getDefaultDays() < 1 || stage.getDefaultDays() > 365) {
                throw invalid("施工阶段工期必须在1至365天之间");
            }
        }
    }

    private CompanyPaymentPlan requirePaymentPlan(Employee current, Long templateId) {
        if (templateId == null) throw invalid("付款计划模板不存在");
        return paymentPlanRepository.findByCompany_ComIdAndTemplateId(current.getCompany().getComId(), templateId)
                .orElseThrow(() -> invalid("付款计划模板不存在或不属于当前企业"));
    }

    private CompanyConstructionPlan requireConstructionPlan(Employee current, Long templateId) {
        if (templateId == null) throw invalid("施工计划模板不存在");
        return constructionPlanRepository.findByCompany_ComIdAndTemplateId(current.getCompany().getComId(), templateId)
                .orElseThrow(() -> invalid("施工计划模板不存在或不属于当前企业"));
    }

    private boolean hasPaymentDefault(Employee current) {
        return paymentPlanRepository.findByCompany_ComIdOrderByDefaultPlanDescSortAscTemplateIdAsc(current.getCompany().getComId()).stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getDefaultPlan()));
    }

    private boolean hasConstructionDefault(Employee current) {
        return constructionPlanRepository.findByCompany_ComIdOrderByDefaultPlanDescSortAscTemplateIdAsc(current.getCompany().getComId()).stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getDefaultPlan()));
    }

    private boolean requiresStage(PaymentTriggerType type) {
        return type == PaymentTriggerType.StageStarted || type == PaymentTriggerType.StageDone
                || type == PaymentTriggerType.StageAccepted;
    }

    private void requireEnabled(PlanTemplateStatus status, String message) {
        if (status != PlanTemplateStatus.Enabled) throw invalid(message);
    }

    private String token() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String textOrNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }
}
