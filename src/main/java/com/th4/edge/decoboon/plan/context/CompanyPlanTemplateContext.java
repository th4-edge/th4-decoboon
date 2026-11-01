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
import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/** 企业自定义付款与施工计划模板维护。 */
@Component
public class CompanyPlanTemplateContext {

    private final TenantContext tenantContext;
    private final PlanTemplateBaselineContext baselineContext;
    private final CompanyPaymentPlanRepository paymentPlanRepository;
    private final CompanyPaymentNodeRepository paymentNodeRepository;
    private final CompanyConstructionPlanRepository constructionPlanRepository;
    private final CompanyConstructionStageRepository constructionStageRepository;
    private final DistributedLock distributedLock;
    private final TransactionTemplate transactionTemplate;

    public CompanyPlanTemplateContext(TenantContext tenantContext,
                                      PlanTemplateBaselineContext baselineContext,
                                      CompanyPaymentPlanRepository paymentPlanRepository,
                                      CompanyPaymentNodeRepository paymentNodeRepository,
                                      CompanyConstructionPlanRepository constructionPlanRepository,
                                      CompanyConstructionStageRepository constructionStageRepository,
                                      DistributedLock distributedLock,
                                      TransactionTemplate transactionTemplate) {
        this.tenantContext = tenantContext;
        this.baselineContext = baselineContext;
        this.paymentPlanRepository = paymentPlanRepository;
        this.paymentNodeRepository = paymentNodeRepository;
        this.constructionPlanRepository = constructionPlanRepository;
        this.constructionStageRepository = constructionStageRepository;
        this.distributedLock = distributedLock;
        this.transactionTemplate = transactionTemplate;
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

    /** 付款/施工计划选择器：数据库分页 + 当前页节点/阶段批量装配。 */
    @Transactional(readOnly = true)
    public PlanTemplateCatalog pickerCatalog(Employee principal, String kind, Integer requestedCurrent,
                                             Integer requestedSize, String keyword) {
        Employee current = tenantContext.requireEmployee(principal);
        Long comId = current.getCompany().getComId();
        int currentPage = requestedCurrent == null || requestedCurrent < 1 ? 1 : requestedCurrent;
        int pageSize = requestedSize == null || requestedSize < 1 ? 20 : Math.min(requestedSize, 50);
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        PlanTemplateCatalog result = new PlanTemplateCatalog();
        result.setCurrent(currentPage);
        result.setSize(pageSize);

        if ("Construction".equalsIgnoreCase(kind)) {
            Specification<CompanyConstructionPlan> spec = (root, query, builder) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(builder.equal(root.get("company").get("comId"), comId));
                predicates.add(builder.equal(root.get("status"), PlanTemplateStatus.Enabled));
                predicates.add(builder.isTrue(root.get("currentEnabled")));
                if (!normalized.isBlank()) {
                    String pattern = "%" + normalized + "%";
                    predicates.add(builder.or(
                            builder.like(builder.lower(root.get("name")), pattern),
                            builder.like(builder.lower(root.get("code")), pattern)
                    ));
                }
                return builder.and(predicates.toArray(Predicate[]::new));
            };
            org.springframework.data.domain.Page<CompanyConstructionPlan> page = constructionPlanRepository.findAll(
                    spec, PageRequest.of(currentPage - 1, pageSize, Sort.by(Sort.Direction.DESC, "defaultPlan").and(Sort.by(Sort.Direction.ASC, "sort"))));
            if (page.getTotalElements() == 0) {
                List<ConstructionPlanTemplate> baselines = baselineContext.catalog().getConstructionPlans().stream()
                        .filter(item -> normalized.isBlank()
                                || (item.getName() != null && item.getName().toLowerCase(Locale.ROOT).contains(normalized))
                                || (item.getCode() != null && item.getCode().toLowerCase(Locale.ROOT).contains(normalized)))
                        .map(this::baselineConstruction)
                        .toList();
                result.setConstructionPlans(baselines);
                result.setTotal(baselines.size());
                result.setHasMore(false);
            } else {
                result.setConstructionPlans(constructionViews(page.getContent()));
                result.setTotal(page.getTotalElements());
                result.setHasMore((long) currentPage * pageSize < page.getTotalElements());
            }
            return result;
        }

        Specification<CompanyPaymentPlan> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("company").get("comId"), comId));
            predicates.add(builder.equal(root.get("status"), PlanTemplateStatus.Enabled));
            predicates.add(builder.isTrue(root.get("currentEnabled")));
            if (!normalized.isBlank()) {
                String pattern = "%" + normalized + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("code")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        org.springframework.data.domain.Page<CompanyPaymentPlan> page = paymentPlanRepository.findAll(
                spec, PageRequest.of(currentPage - 1, pageSize, Sort.by(Sort.Direction.DESC, "defaultPlan").and(Sort.by(Sort.Direction.ASC, "sort"))));
        if (page.getTotalElements() == 0) {
            List<PaymentPlanTemplate> baselines = baselineContext.catalog().getPaymentPlans().stream()
                    .filter(item -> normalized.isBlank()
                            || (item.getName() != null && item.getName().toLowerCase(Locale.ROOT).contains(normalized))
                            || (item.getCode() != null && item.getCode().toLowerCase(Locale.ROOT).contains(normalized)))
                    .map(this::baselinePayment)
                    .toList();
            result.setPaymentPlans(baselines);
            result.setTotal(baselines.size());
            result.setHasMore(false);
        } else {
            result.setPaymentPlans(paymentViews(page.getContent()));
            result.setTotal(page.getTotalElements());
            result.setHasMore((long) currentPage * pageSize < page.getTotalElements());
        }
        return result;
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
        plan.setCurrentEnabled(Boolean.FALSE);
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
        CompanyPaymentPlan plan = requirePaymentDraft(current, request == null ? null : request.getTemplateId());
        List<CompanyPaymentNode> nodes = paymentNodes(plan, request.getNodes());
        applyPayment(plan, request);
        validatePayment(plan, nodes);
        plan = paymentPlanRepository.save(plan);
        replacePaymentNodes(plan, nodes);
        return paymentView(plan);
    }

    public PaymentPlanTemplate buildPayment(Employee principal, CompanyPaymentPlan request) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        CompanyPaymentPlan base = requirePaymentPlan(current, request == null ? null : request.getTemplateId());
        String lockKey = planLockKey("payment", comId, base.getCode());
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                CompanyPaymentPlan latest = paymentPlanRepository
                        .findFirstByCompany_ComIdAndCodeOrderByVersionDesc(comId, base.getCode())
                        .orElseThrow(() -> new DefinedException(new DefaultErrorCode("409", "付款计划版本已变化，请刷新后重试")));
                String requestedBaselineCode = request == null ? null : request.getSourceBaselineCode();
                if (latest.getStatus() == PlanTemplateStatus.Draft) {
                    if (!hasText(requestedBaselineCode)
                            || requestedBaselineCode.equals(latest.getSourceBaselineCode())) {
                        return paymentView(latest);
                    }
                    throw new DefinedException(new DefaultErrorCode("409", "付款计划已存在其他草稿，请直接编辑"));
                }
                if (!latest.getTemplateId().equals(base.getTemplateId())) {
                    throw new DefinedException(new DefaultErrorCode("409", "付款计划版本已变化，请刷新后重试"));
                }
                CompanyPaymentPlan draft = new CompanyPaymentPlan();
                draft.setCompany(current.getCompany());
                draft.setCode(latest.getCode());
                draft.setVersion(latest.getVersion() + 1);
                draft.setStatus(PlanTemplateStatus.Draft);
                draft.setDefaultPlan(Boolean.FALSE);
                draft.setCurrentEnabled(Boolean.FALSE);
                draft.setName(latest.getName());
                draft.setDescription(latest.getDescription());
                draft.setSort(latest.getSort());
                draft.setSourceBaselineCode(latest.getSourceBaselineCode());
                draft.setSourceBaselineVersion(latest.getSourceBaselineVersion());
                List<CompanyPaymentNode> nodes;
                if (request != null && hasText(request.getSourceBaselineCode())) {
                    PaymentPlanTemplate baseline = baselineContext.paymentPlan(request.getSourceBaselineCode());
                    draft.setSourceBaselineCode(baseline.getCode());
                    draft.setSourceBaselineVersion(baseline.getVersion());
                    nodes = paymentNodesFromBaseline(draft, baseline.getNodes());
                } else {
                    nodes = copyPaymentNodes(latest, draft);
                }
                validatePayment(draft, nodes);
                draft = paymentPlanRepository.save(draft);
                replacePaymentNodes(draft, nodes);
                return paymentView(draft);
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    public PaymentPlanTemplate publishPayment(Employee principal, CompanyPaymentPlan request) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        CompanyPaymentPlan base = requirePaymentPlan(current, request == null ? null : request.getTemplateId());
        String lockKey = planDefaultLockKey("payment", comId);
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                CompanyPaymentPlan plan = requirePaymentPlan(current, base.getTemplateId());
                if (plan.getStatus() == PlanTemplateStatus.Enabled && Boolean.TRUE.equals(plan.getCurrentEnabled())) {
                    return paymentView(plan);
                }
                if (plan.getStatus() != PlanTemplateStatus.Draft) {
                    throw new DefinedException(new DefaultErrorCode("409", "只有草稿付款计划可以启用"));
                }
                CompanyPaymentPlan latest = paymentPlanRepository
                        .findFirstByCompany_ComIdAndCodeOrderByVersionDesc(comId, plan.getCode())
                        .orElse(plan);
                if (!latest.getTemplateId().equals(plan.getTemplateId())) {
                    throw new DefinedException(new DefaultErrorCode("409", "只能启用付款计划最新版本"));
                }
                List<CompanyPaymentNode> nodes = paymentNodeRepository.findByPlan_TemplateIdOrderBySortAscNodeIdAsc(plan.getTemplateId());
                validatePayment(plan, nodes);
                List<CompanyPaymentPlan> previous = paymentPlanRepository
                        .findByCompany_ComIdAndCodeAndCurrentEnabledTrue(comId, plan.getCode());
                boolean inheritedDefault = previous.stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.getDefaultPlan()));
                List<CompanyPaymentPlan> allCurrent = paymentPlanRepository
                        .findByCompany_ComIdAndCurrentEnabledTrue(comId);
                boolean otherDefault = allCurrent.stream()
                        .filter(item -> !item.getTemplateId().equals(plan.getTemplateId()))
                        .anyMatch(item -> Boolean.TRUE.equals(item.getDefaultPlan()));
                for (CompanyPaymentPlan item : previous) {
                    if (!item.getTemplateId().equals(plan.getTemplateId())) {
                        item.setCurrentEnabled(Boolean.FALSE);
                        item.setDefaultPlan(Boolean.FALSE);
                        paymentPlanRepository.save(item);
                    }
                }
                if (inheritedDefault) {
                    for (CompanyPaymentPlan item : allCurrent) {
                        if (!item.getTemplateId().equals(plan.getTemplateId())
                                && Boolean.TRUE.equals(item.getDefaultPlan())) {
                            item.setDefaultPlan(Boolean.FALSE);
                            paymentPlanRepository.save(item);
                        }
                    }
                }
                plan.setStatus(PlanTemplateStatus.Enabled);
                plan.setCurrentEnabled(Boolean.TRUE);
                plan.setPublishedAt(LocalDateTime.now());
                plan.setDefaultPlan(inheritedDefault || !otherDefault);
                paymentPlanRepository.save(plan);
                return paymentView(plan);
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    public PaymentPlanTemplate disablePayment(Employee principal, CompanyPaymentPlan request) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        CompanyPaymentPlan base = requirePaymentPlan(current, request == null ? null : request.getTemplateId());
        String lockKey = planLockKey("payment", comId, base.getCode());
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                CompanyPaymentPlan plan = requirePaymentPlan(current, base.getTemplateId());
                if (plan.getStatus() == PlanTemplateStatus.Disabled) {
                    plan.setCurrentEnabled(Boolean.FALSE);
                    plan.setDefaultPlan(Boolean.FALSE);
                    return paymentView(paymentPlanRepository.save(plan));
                }
                if (plan.getStatus() != PlanTemplateStatus.Enabled || !Boolean.TRUE.equals(plan.getCurrentEnabled())) {
                    throw new DefinedException(new DefaultErrorCode("409", "只有当前启用的付款计划可以停用"));
                }
                plan.setStatus(PlanTemplateStatus.Disabled);
                plan.setCurrentEnabled(Boolean.FALSE);
                plan.setDefaultPlan(Boolean.FALSE);
                return paymentView(paymentPlanRepository.save(plan));
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    public PaymentPlanTemplate defaultPayment(Employee principal, CompanyPaymentPlan request) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        CompanyPaymentPlan plan = requirePaymentPlan(current, request == null ? null : request.getTemplateId());
        String lockKey = planDefaultLockKey("payment", comId);
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                CompanyPaymentPlan target = requirePaymentPlan(current, plan.getTemplateId());
                requireEnabled(target.getStatus(), "只有已启用的付款计划模板可以设为默认");
                if (!Boolean.TRUE.equals(target.getCurrentEnabled())) {
                    throw new DefinedException(new DefaultErrorCode("409", "只有当前启用的付款计划可以设为默认"));
                }
                for (CompanyPaymentPlan item : paymentPlanRepository.findByCompany_ComIdAndCurrentEnabledTrue(comId)) {
                    item.setDefaultPlan(item.getTemplateId().equals(target.getTemplateId()));
                    paymentPlanRepository.save(item);
                }
                return paymentView(target);
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
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
        plan.setCurrentEnabled(Boolean.FALSE);
        List<CompanyConstructionStage> stages;
        if (hasText(request.getSourceBaselineCode())) {
            ConstructionPlanTemplate baseline = baselineContext.constructionPlan(request.getSourceBaselineCode());
            plan.setName(hasText(request.getName()) ? request.getName() : baseline.getName() + "（企业版）");
            plan.setDescription(hasText(request.getDescription()) ? request.getDescription() : baseline.getDescription());
            plan.setSort(request.getSort() == null ? baseline.getSort() : request.getSort());
            // DEC-048 T-02：复制基线时保留模板声明的标准总工期（如八阶段 120 天），不得被阶段汇总覆盖。
            plan.setTotalDays(baseline.getTotalDays());
            plan.setSourceBaselineCode(baseline.getCode());
            plan.setSourceBaselineVersion(baseline.getVersion());
            stages = constructionStagesFromBaseline(plan, baseline.getStages());
        } else {
            applyConstruction(plan, request);
            stages = constructionStages(plan, request.getStages());
            // 空白新建未声明总工期时，才以阶段相对权重合计作为初始值。
            if (request.getTotalDays() == null) {
                plan.setTotalDays(stages.stream().mapToInt(item -> Math.max(1, item.getDefaultDays())).sum());
            }
        }
        validateConstruction(plan, stages);
        plan = constructionPlanRepository.save(plan);
        replaceConstructionStages(plan, stages);
        return constructionView(plan);
    }

    @Transactional
    public ConstructionPlanTemplate updateConstruction(Employee principal, CompanyConstructionPlan request) {
        Employee current = requireMaintainer(principal);
        CompanyConstructionPlan plan = requireConstructionDraft(current, request == null ? null : request.getTemplateId());
        List<CompanyConstructionStage> stages = constructionStages(plan, request.getStages());
        applyConstruction(plan, request);
        // DEC-048 T-02：保留模板已声明的标准总工期；仅历史数据为空时兜底。
        if (plan.getTotalDays() == null) {
            plan.setTotalDays(stages.stream().mapToInt(item -> Math.max(1, item.getDefaultDays())).sum());
        }
        validateConstruction(plan, stages);
        plan = constructionPlanRepository.save(plan);
        replaceConstructionStages(plan, stages);
        return constructionView(plan);
    }

    public ConstructionPlanTemplate buildConstruction(Employee principal, CompanyConstructionPlan request) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        CompanyConstructionPlan base = requireConstructionPlan(current, request == null ? null : request.getTemplateId());
        String lockKey = planLockKey("construction", comId, base.getCode());
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                CompanyConstructionPlan latest = constructionPlanRepository
                        .findFirstByCompany_ComIdAndCodeOrderByVersionDesc(comId, base.getCode())
                        .orElseThrow(() -> new DefinedException(new DefaultErrorCode("409", "施工计划版本已变化，请刷新后重试")));
                String requestedBaselineCode = request == null ? null : request.getSourceBaselineCode();
                if (latest.getStatus() == PlanTemplateStatus.Draft) {
                    if (!hasText(requestedBaselineCode)
                            || requestedBaselineCode.equals(latest.getSourceBaselineCode())) {
                        return constructionView(latest);
                    }
                    throw new DefinedException(new DefaultErrorCode("409", "施工计划已存在其他草稿，请直接编辑"));
                }
                if (!latest.getTemplateId().equals(base.getTemplateId())) {
                    throw new DefinedException(new DefaultErrorCode("409", "施工计划版本已变化，请刷新后重试"));
                }
                CompanyConstructionPlan draft = new CompanyConstructionPlan();
                draft.setCompany(current.getCompany());
                draft.setCode(latest.getCode());
                draft.setVersion(latest.getVersion() + 1);
                draft.setStatus(PlanTemplateStatus.Draft);
                draft.setDefaultPlan(Boolean.FALSE);
                draft.setCurrentEnabled(Boolean.FALSE);
                draft.setName(latest.getName());
                draft.setDescription(latest.getDescription());
                draft.setTotalDays(latest.getTotalDays());
                draft.setSort(latest.getSort());
                draft.setSourceBaselineCode(latest.getSourceBaselineCode());
                draft.setSourceBaselineVersion(latest.getSourceBaselineVersion());
                List<CompanyConstructionStage> stages;
                if (request != null && hasText(request.getSourceBaselineCode())) {
                    ConstructionPlanTemplate baseline = baselineContext.constructionPlan(request.getSourceBaselineCode());
                    draft.setSourceBaselineCode(baseline.getCode());
                    draft.setSourceBaselineVersion(baseline.getVersion());
                    draft.setTotalDays(baseline.getTotalDays());
                    stages = constructionStagesFromBaseline(draft, baseline.getStages());
                } else {
                    stages = copyConstructionStages(latest, draft);
                }
                validateConstruction(draft, stages);
                draft = constructionPlanRepository.save(draft);
                replaceConstructionStages(draft, stages);
                return constructionView(draft);
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    public ConstructionPlanTemplate publishConstruction(Employee principal, CompanyConstructionPlan request) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        CompanyConstructionPlan base = requireConstructionPlan(current, request == null ? null : request.getTemplateId());
        String lockKey = planDefaultLockKey("construction", comId);
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                CompanyConstructionPlan plan = requireConstructionPlan(current, base.getTemplateId());
                if (plan.getStatus() == PlanTemplateStatus.Enabled && Boolean.TRUE.equals(plan.getCurrentEnabled())) {
                    return constructionView(plan);
                }
                if (plan.getStatus() != PlanTemplateStatus.Draft) {
                    throw new DefinedException(new DefaultErrorCode("409", "只有草稿施工计划可以启用"));
                }
                CompanyConstructionPlan latest = constructionPlanRepository
                        .findFirstByCompany_ComIdAndCodeOrderByVersionDesc(comId, plan.getCode())
                        .orElse(plan);
                if (!latest.getTemplateId().equals(plan.getTemplateId())) {
                    throw new DefinedException(new DefaultErrorCode("409", "只能启用施工计划最新版本"));
                }
                List<CompanyConstructionStage> stages = constructionStageRepository.findByPlan_TemplateIdOrderBySortAscStageIdAsc(plan.getTemplateId());
                validateConstruction(plan, stages);
                List<CompanyConstructionPlan> previous = constructionPlanRepository
                        .findByCompany_ComIdAndCodeAndCurrentEnabledTrue(comId, plan.getCode());
                boolean inheritedDefault = previous.stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.getDefaultPlan()));
                List<CompanyConstructionPlan> allCurrent = constructionPlanRepository
                        .findByCompany_ComIdAndCurrentEnabledTrue(comId);
                boolean otherDefault = allCurrent.stream()
                        .filter(item -> !item.getTemplateId().equals(plan.getTemplateId()))
                        .anyMatch(item -> Boolean.TRUE.equals(item.getDefaultPlan()));
                for (CompanyConstructionPlan item : previous) {
                    if (!item.getTemplateId().equals(plan.getTemplateId())) {
                        item.setCurrentEnabled(Boolean.FALSE);
                        item.setDefaultPlan(Boolean.FALSE);
                        constructionPlanRepository.save(item);
                    }
                }
                if (inheritedDefault) {
                    for (CompanyConstructionPlan item : allCurrent) {
                        if (!item.getTemplateId().equals(plan.getTemplateId())
                                && Boolean.TRUE.equals(item.getDefaultPlan())) {
                            item.setDefaultPlan(Boolean.FALSE);
                            constructionPlanRepository.save(item);
                        }
                    }
                }
                plan.setStatus(PlanTemplateStatus.Enabled);
                plan.setCurrentEnabled(Boolean.TRUE);
                plan.setPublishedAt(LocalDateTime.now());
                plan.setDefaultPlan(inheritedDefault || !otherDefault);
                constructionPlanRepository.save(plan);
                return constructionView(plan);
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    public ConstructionPlanTemplate disableConstruction(Employee principal, CompanyConstructionPlan request) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        CompanyConstructionPlan base = requireConstructionPlan(current, request == null ? null : request.getTemplateId());
        String lockKey = planLockKey("construction", comId, base.getCode());
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                CompanyConstructionPlan plan = requireConstructionPlan(current, base.getTemplateId());
                if (plan.getStatus() == PlanTemplateStatus.Disabled) {
                    plan.setCurrentEnabled(Boolean.FALSE);
                    plan.setDefaultPlan(Boolean.FALSE);
                    return constructionView(constructionPlanRepository.save(plan));
                }
                if (plan.getStatus() != PlanTemplateStatus.Enabled || !Boolean.TRUE.equals(plan.getCurrentEnabled())) {
                    throw new DefinedException(new DefaultErrorCode("409", "只有当前启用的施工计划可以停用"));
                }
                plan.setStatus(PlanTemplateStatus.Disabled);
                plan.setCurrentEnabled(Boolean.FALSE);
                plan.setDefaultPlan(Boolean.FALSE);
                return constructionView(constructionPlanRepository.save(plan));
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    public ConstructionPlanTemplate defaultConstruction(Employee principal, CompanyConstructionPlan request) {
        Employee current = requireMaintainer(principal);
        Long comId = current.getCompany().getComId();
        CompanyConstructionPlan plan = requireConstructionPlan(current, request == null ? null : request.getTemplateId());
        String lockKey = planDefaultLockKey("construction", comId);
        lock(lockKey);
        try {
            return executeVersionWrite(() -> {
                CompanyConstructionPlan target = requireConstructionPlan(current, plan.getTemplateId());
                requireEnabled(target.getStatus(), "只有已启用的施工计划模板可以设为默认");
                if (!Boolean.TRUE.equals(target.getCurrentEnabled())) {
                    throw new DefinedException(new DefaultErrorCode("409", "只有当前启用的施工计划可以设为默认"));
                }
                for (CompanyConstructionPlan item : constructionPlanRepository.findByCompany_ComIdAndCurrentEnabledTrue(comId)) {
                    item.setDefaultPlan(item.getTemplateId().equals(target.getTemplateId()));
                    constructionPlanRepository.save(item);
                }
                return constructionView(target);
            });
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Transactional(readOnly = true)
    public PaymentPlanTemplate resolvePaymentPlan(Long comId, String code) {
        if (!hasText(code)) {
            CompanyPaymentPlan defaultPlan = paymentPlanRepository
                    .findByCompany_ComIdAndStatusAndCurrentEnabledTrueOrderByDefaultPlanDescTemplateIdAsc(
                            comId, PlanTemplateStatus.Enabled).stream()
                    .filter(item -> Boolean.TRUE.equals(item.getDefaultPlan()))
                    .findFirst()
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "请选择明确的付款计划模板")));
            return paymentView(defaultPlan);
        }
        CompanyPaymentPlan custom = paymentPlanRepository
                .findByCompany_ComIdAndCodeAndCurrentEnabledTrue(comId, code).stream()
                .filter(item -> item.getStatus() == PlanTemplateStatus.Enabled).findFirst().orElse(null);
        if (custom != null) return paymentView(custom);
        return baselineContext.paymentPlan(code);
    }

    @Transactional(readOnly = true)
    public ConstructionPlanTemplate resolveConstructionPlan(Long comId, String code) {
        if (!hasText(code)) {
            CompanyConstructionPlan defaultPlan = constructionPlanRepository
                    .findByCompany_ComIdAndStatusAndCurrentEnabledTrueOrderByDefaultPlanDescTemplateIdAsc(
                            comId, PlanTemplateStatus.Enabled).stream()
                    .filter(item -> Boolean.TRUE.equals(item.getDefaultPlan()))
                    .findFirst()
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "请选择明确的施工计划模板")));
            return constructionView(defaultPlan);
        }
        CompanyConstructionPlan custom = constructionPlanRepository
                .findByCompany_ComIdAndCodeAndCurrentEnabledTrue(comId, code).stream()
                .filter(item -> item.getStatus() == PlanTemplateStatus.Enabled).findFirst().orElse(null);
        if (custom != null) return constructionView(custom);
        return baselineContext.constructionPlan(code);
    }

    private PlanTemplateCatalog catalog(Employee current, boolean includeDisabled) {
        Long comId = current.getCompany().getComId();
        List<PaymentPlanTemplate> payments = new ArrayList<>();
        List<CompanyPaymentPlan> customPayments = includeDisabled
                ? paymentPlanRepository.findByCompany_ComIdOrderByCodeAscVersionDescTemplateIdDesc(comId)
                : paymentPlanRepository.findByCompany_ComIdAndStatusAndCurrentEnabledTrueOrderByDefaultPlanDescTemplateIdAsc(
                        comId, PlanTemplateStatus.Enabled);
        customPayments.forEach(item -> payments.add(paymentView(item)));
        baselineContext.catalog().getPaymentPlans().forEach(item -> payments.add(baselinePayment(item)));

        List<ConstructionPlanTemplate> constructions = new ArrayList<>();
        List<CompanyConstructionPlan> customConstructions = includeDisabled
                ? constructionPlanRepository.findByCompany_ComIdOrderByCodeAscVersionDescTemplateIdDesc(comId)
                : constructionPlanRepository.findByCompany_ComIdAndStatusAndCurrentEnabledTrueOrderByDefaultPlanDescTemplateIdAsc(
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

    private List<PaymentPlanTemplate> paymentViews(List<CompanyPaymentPlan> plans) {
        if (plans.isEmpty()) return List.of();
        List<Long> ids = plans.stream().map(CompanyPaymentPlan::getTemplateId).toList();
        Map<Long, List<PaymentPlanNodeTemplate>> nodes = new HashMap<>();
        for (CompanyPaymentNode node : paymentNodeRepository.findByPlan_TemplateIdInOrderByPlanTemplateIdAscSortAscNodeIdAsc(ids)) {
            nodes.computeIfAbsent(node.getPlan().getTemplateId(), ignored -> new ArrayList<>()).add(paymentNodeView(node));
        }
        return plans.stream().map(plan -> paymentView(plan, nodes.getOrDefault(plan.getTemplateId(), List.of()))).toList();
    }

    private PaymentPlanTemplate paymentView(CompanyPaymentPlan plan, List<PaymentPlanNodeTemplate> nodes) {
        PaymentPlanTemplate view = new PaymentPlanTemplate();
        view.setTemplateId(plan.getTemplateId());
        view.setCode(plan.getCode());
        view.setVersion(plan.getVersion());
        view.setName(plan.getName());
        view.setDescription(plan.getDescription());
        view.setSort(plan.getSort());
        view.setSource(PlanTemplateSource.Company);
        view.setSourceBaselineCode(plan.getSourceBaselineCode());
        view.setSourceBaselineVersion(plan.getSourceBaselineVersion());
        view.setStatus(plan.getStatus());
        view.setDefaultPlan(Boolean.TRUE.equals(plan.getDefaultPlan()));
        view.setCurrentEnabled(Boolean.TRUE.equals(plan.getCurrentEnabled()));
        view.setNodes(nodes);
        return view;
    }

    private List<ConstructionPlanTemplate> constructionViews(List<CompanyConstructionPlan> plans) {
        if (plans.isEmpty()) return List.of();
        List<Long> ids = plans.stream().map(CompanyConstructionPlan::getTemplateId).toList();
        Map<Long, List<ConstructionStageTemplate>> stages = new HashMap<>();
        for (CompanyConstructionStage stage : constructionStageRepository.findByPlan_TemplateIdInOrderByPlanTemplateIdAscSortAscStageIdAsc(ids)) {
            stages.computeIfAbsent(stage.getPlan().getTemplateId(), ignored -> new ArrayList<>()).add(constructionStageView(stage));
        }
        return plans.stream().map(plan -> constructionView(plan, stages.getOrDefault(plan.getTemplateId(), List.of()))).toList();
    }

    private ConstructionPlanTemplate constructionView(CompanyConstructionPlan plan, List<ConstructionStageTemplate> stages) {
        ConstructionPlanTemplate view = new ConstructionPlanTemplate();
        view.setTemplateId(plan.getTemplateId());
        view.setCode(plan.getCode());
        view.setVersion(plan.getVersion());
        view.setName(plan.getName());
        view.setDescription(plan.getDescription());
        view.setTotalDays(plan.getTotalDays());
        view.setSort(plan.getSort());
        view.setSource(PlanTemplateSource.Company);
        view.setSourceBaselineCode(plan.getSourceBaselineCode());
        view.setSourceBaselineVersion(plan.getSourceBaselineVersion());
        view.setStatus(plan.getStatus());
        view.setDefaultPlan(Boolean.TRUE.equals(plan.getDefaultPlan()));
        view.setCurrentEnabled(Boolean.TRUE.equals(plan.getCurrentEnabled()));
        view.setStages(stages);
        return view;
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
        view.setSourceBaselineCode(plan.getSourceBaselineCode());
        view.setSourceBaselineVersion(plan.getSourceBaselineVersion());
        view.setStatus(plan.getStatus());
        view.setDefaultPlan(Boolean.TRUE.equals(plan.getDefaultPlan()));
        view.setCurrentEnabled(Boolean.TRUE.equals(plan.getCurrentEnabled()));
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
        view.setSourceBaselineCode(plan.getSourceBaselineCode());
        view.setSourceBaselineVersion(plan.getSourceBaselineVersion());
        view.setStatus(plan.getStatus());
        view.setDefaultPlan(Boolean.TRUE.equals(plan.getDefaultPlan()));
        view.setCurrentEnabled(Boolean.TRUE.equals(plan.getCurrentEnabled()));
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
        if (request.getTotalDays() != null) plan.setTotalDays(request.getTotalDays());
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
        if (plan.getTotalDays() == null || plan.getTotalDays() < 1) throw invalid("施工计划标准总工期必须大于0");
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

    private CompanyPaymentPlan requirePaymentDraft(Employee current, Long templateId) {
        CompanyPaymentPlan plan = requirePaymentPlan(current, templateId);
        if (plan.getStatus() != PlanTemplateStatus.Draft) {
            throw new DefinedException(new DefaultErrorCode("409", "已启用或已停用的付款计划不能原地修改，请先创建新版本"));
        }
        CompanyPaymentPlan latest = paymentPlanRepository
                .findFirstByCompany_ComIdAndCodeOrderByVersionDesc(current.getCompany().getComId(), plan.getCode())
                .orElse(plan);
        if (!latest.getTemplateId().equals(plan.getTemplateId())) {
            throw new DefinedException(new DefaultErrorCode("409", "只能修改付款计划最新草稿"));
        }
        return plan;
    }

    private CompanyConstructionPlan requireConstructionDraft(Employee current, Long templateId) {
        CompanyConstructionPlan plan = requireConstructionPlan(current, templateId);
        if (plan.getStatus() != PlanTemplateStatus.Draft) {
            throw new DefinedException(new DefaultErrorCode("409", "已启用或已停用的施工计划不能原地修改，请先创建新版本"));
        }
        CompanyConstructionPlan latest = constructionPlanRepository
                .findFirstByCompany_ComIdAndCodeOrderByVersionDesc(current.getCompany().getComId(), plan.getCode())
                .orElse(plan);
        if (!latest.getTemplateId().equals(plan.getTemplateId())) {
            throw new DefinedException(new DefaultErrorCode("409", "只能修改施工计划最新草稿"));
        }
        return plan;
    }

    private List<CompanyPaymentNode> copyPaymentNodes(CompanyPaymentPlan source, CompanyPaymentPlan target) {
        List<CompanyPaymentNode> result = new ArrayList<>();
        for (CompanyPaymentNode item : paymentNodeRepository.findByPlan_TemplateIdOrderBySortAscNodeIdAsc(source.getTemplateId())) {
            CompanyPaymentNode copy = new CompanyPaymentNode();
            copy.setCompany(target.getCompany());
            copy.setPlan(target);
            copy.setCode(item.getCode());
            copy.setName(item.getName());
            copy.setRatio(item.getRatio());
            copy.setType(item.getType());
            copy.setTriggerType(item.getTriggerType());
            copy.setTriggerStageCode(item.getTriggerStageCode());
            copy.setOffsetDays(item.getOffsetDays());
            copy.setSort(item.getSort());
            result.add(copy);
        }
        return result;
    }

    private List<CompanyConstructionStage> copyConstructionStages(CompanyConstructionPlan source, CompanyConstructionPlan target) {
        List<CompanyConstructionStage> result = new ArrayList<>();
        for (CompanyConstructionStage item : constructionStageRepository.findByPlan_TemplateIdOrderBySortAscStageIdAsc(source.getTemplateId())) {
            CompanyConstructionStage copy = new CompanyConstructionStage();
            copy.setCompany(target.getCompany());
            copy.setPlan(target);
            copy.setCode(item.getCode());
            copy.setName(item.getName());
            copy.setDefaultDays(item.getDefaultDays());
            copy.setAcceptanceMode(item.getAcceptanceMode());
            copy.setSort(item.getSort());
            copy.setDescription(item.getDescription());
            result.add(copy);
        }
        return result;
    }

    private void lock(String lockKey) {
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "计划模板版本正在被处理，请稍后重试"));
        }
    }

    private String planLockKey(String type, Long comId, String code) {
        return "company-plan-template:" + type + ":" + comId + ":" + code;
    }

    private String planDefaultLockKey(String type, Long comId) {
        return "company-plan-template:" + type + ":" + comId + ":default";
    }

    private <T> T executeVersionWrite(Supplier<T> action) {
        try {
            return transactionTemplate.execute(status -> action.get());
        } catch (DataIntegrityViolationException ex) {
            throw new DefinedException(new DefaultErrorCode("409", "计划模板版本正在被其他操作更新，请刷新后重试"));
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
