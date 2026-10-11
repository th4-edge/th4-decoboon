package com.th4.edge.decoboon.plan.context;

import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.plan.configure.PaymentTriggerType;
import com.th4.edge.decoboon.plan.configure.PlanSnapshotStatus;
import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.ConstructionStageTemplate;
import com.th4.edge.decoboon.plan.model.ContractConstructionPlan;
import com.th4.edge.decoboon.plan.model.ContractConstructionStage;
import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
import com.th4.edge.decoboon.plan.model.PaymentPlanNodeTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.decoboon.plan.repository.ContractConstructionPlanRepository;
import com.th4.edge.decoboon.plan.repository.ContractConstructionStageRepository;
import com.th4.edge.decoboon.plan.repository.ContractPaymentNodeRepository;
import com.th4.edge.decoboon.plan.repository.ContractPaymentPlanRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 合同级付款计划和施工计划快照。 */
@Component
public class ContractPlanContext {

    private final CompanyPlanTemplateContext templateContext;
    private final ContractPaymentPlanRepository paymentPlanRepository;
    private final ContractPaymentNodeRepository paymentNodeRepository;
    private final ContractConstructionPlanRepository constructionPlanRepository;
    private final ContractConstructionStageRepository constructionStageRepository;

    public ContractPlanContext(CompanyPlanTemplateContext templateContext,
                               ContractPaymentPlanRepository paymentPlanRepository,
                               ContractPaymentNodeRepository paymentNodeRepository,
                               ContractConstructionPlanRepository constructionPlanRepository,
                               ContractConstructionStageRepository constructionStageRepository) {
        this.templateContext = templateContext;
        this.paymentPlanRepository = paymentPlanRepository;
        this.paymentNodeRepository = paymentNodeRepository;
        this.constructionPlanRepository = constructionPlanRepository;
        this.constructionStageRepository = constructionStageRepository;
    }

    /**
     * DEC-048 CONTRACT-TERM-01/02/T-03：签约时锁定合同基准工期。
     *
     * <p>计划开工日期必填；基准完工日不由人工录入，按施工计划模板标准总工期自动生成
     * （未指定模板时使用平台基线，如 {@code RESIDENTIAL_STANDARD_8} = 120 天），生成后即锁定；
     * 后续只有 Boss/店长按 CONTRACT-TERM-03 特批覆盖才会形成新基准。</p>
     */
    @Transactional
    public void lockBaselineTerm(ProjectContract contract, String constructionPlanCode) {
        if (contract.getStartDate() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "确认签约必须填写计划开工日期"));
        }
        if (contract.getPlanEndDate() == null) {
            ConstructionPlanTemplate template = templateContext.resolveConstructionPlan(
                    contract.getCompany().getComId(), constructionPlanCode);
            long standardDays = Math.max(1, template.getTotalDays());
            contract.setPlanEndDate(contract.getStartDate().plusDays(standardDays - 1L));
        }
    }

    @Transactional
    public void select(ProjectContract contract, String paymentPlanCode, String constructionPlanCode) {
        if (contract == null || contract.getContractId() == null) {
            throw invalid("合同不存在，无法选择付款和施工计划");
        }
        if (contract.getProject() != null || contract.getStatus() == ContractStatus.Effective
                || contract.getStatus() == ContractStatus.Active || contract.getStatus() == ContractStatus.Changed) {
            throw invalid("合同生效后不能更换付款或施工计划");
        }
        ContractConstructionPlan existingConstruction = constructionPlanRepository
                .findByContract_ContractIdAndCompany_ComId(contract.getContractId(), contract.getCompany().getComId())
                .orElse(null);
        ContractPaymentPlan existingPayment = paymentPlanRepository
                .findByContract_ContractIdAndCompany_ComId(contract.getContractId(), contract.getCompany().getComId())
                .orElse(null);
        if (existingPayment != null && planCode(paymentPlanCode) != null
                && !existingPayment.getSourceCode().equals(planCode(paymentPlanCode))) {
            paymentNodeRepository.deleteByPlan_PlanId(existingPayment.getPlanId());
            paymentPlanRepository.delete(existingPayment);
            paymentPlanRepository.flush();
            existingPayment = null;
        }
        if (existingConstruction != null && planCode(constructionPlanCode) != null
                && !existingConstruction.getSourceCode().equals(planCode(constructionPlanCode))) {
            constructionStageRepository.deleteByPlan_PlanId(existingConstruction.getPlanId());
            constructionPlanRepository.delete(existingConstruction);
            constructionPlanRepository.flush();
            existingConstruction = null;
        }
        if (existingConstruction == null) createConstruction(contract, constructionPlanCode);
        if (existingPayment == null) createPayment(contract, paymentPlanCode);
    }

    @Transactional
    public void ensure(ProjectContract contract) {
        ContractPaymentPlan payment = paymentPlanRepository.findByContract_ContractIdAndCompany_ComId(
                contract.getContractId(), contract.getCompany().getComId()).orElse(null);
        ContractConstructionPlan construction = constructionPlanRepository.findByContract_ContractIdAndCompany_ComId(
                contract.getContractId(), contract.getCompany().getComId()).orElse(null);
        // 合同已生效时 ensure 可能被重复调用；已存在两份快照即幂等返回，不能再次走更换计划校验。
        if (payment != null && construction != null) {
            return;
        }
        select(contract, payment == null ? null : payment.getSourceCode(),
                construction == null ? null : construction.getSourceCode());
    }

    public ContractPaymentPlan paymentPlan(ProjectContract contract) {
        if (contract == null || contract.getContractId() == null) return null;
        ContractPaymentPlan plan = paymentPlanRepository.findByContract_ContractIdAndCompany_ComId(
                        contract.getContractId(), contract.getCompany().getComId())
                .orElse(null);
        if (plan != null) plan.setNodes(paymentNodeRepository.findByPlan_PlanIdOrderBySortAsc(plan.getPlanId()));
        return plan;
    }

    public ContractConstructionPlan constructionPlan(ProjectContract contract) {
        if (contract == null || contract.getContractId() == null) return null;
        ContractConstructionPlan plan = constructionPlanRepository.findByContract_ContractIdAndCompany_ComId(
                        contract.getContractId(), contract.getCompany().getComId())
                .orElse(null);
        if (plan != null) plan.setStages(constructionStageRepository.findByPlan_PlanIdOrderBySortAsc(plan.getPlanId()));
        return plan;
    }

    /**
     * 以项目排期为准回写合同施工计划快照（用户 2026-09-23 裁决：排期与快照冲突时以排期为准）。
     *
     * <p>按阶段编码匹配快照阶段，用项目阶段的计划起止覆盖快照；快照整体起止取各阶段最值。</p>
     */
    @Transactional
    public void syncConstructionPlanFromProject(ProjectContract contract, List<ProjectStage> stages) {
        if (contract == null || contract.getContractId() == null || contract.getCompany() == null
                || stages == null || stages.isEmpty()) {
            return;
        }
        ContractConstructionPlan plan = constructionPlanRepository
                .findByContract_ContractIdAndCompany_ComId(contract.getContractId(), contract.getCompany().getComId())
                .orElse(null);
        if (plan == null) {
            return;
        }
        Map<String, ProjectStage> byCode = new HashMap<>();
        for (ProjectStage stage : stages) {
            if (stage.getStageCode() != null && !stage.getStageCode().isBlank()) {
                byCode.put(stage.getStageCode(), stage);
            }
        }
        List<ContractConstructionStage> snapshots = constructionStageRepository.findByPlan_PlanIdOrderBySortAsc(plan.getPlanId());
        LocalDate start = null;
        LocalDate end = null;
        for (ContractConstructionStage snapshot : snapshots) {
            ProjectStage stage = snapshot.getCode() == null ? null : byCode.get(snapshot.getCode());
            if (stage == null) {
                continue;
            }
            if (stage.getPlannedStart() != null) {
                snapshot.setPlannedStart(stage.getPlannedStart());
            }
            if (stage.getPlannedEnd() != null) {
                snapshot.setPlannedEnd(stage.getPlannedEnd());
            }
            if (snapshot.getPlannedStart() != null && (start == null || snapshot.getPlannedStart().isBefore(start))) {
                start = snapshot.getPlannedStart();
            }
            if (snapshot.getPlannedEnd() != null && (end == null || snapshot.getPlannedEnd().isAfter(end))) {
                end = snapshot.getPlannedEnd();
            }
        }
        if (!snapshots.isEmpty()) {
            constructionStageRepository.saveAll(snapshots);
        }
        if (start != null) {
            plan.setStartDate(start);
        }
        if (end != null) {
            plan.setEndDate(end);
        }
        constructionPlanRepository.save(plan);
    }

    /**
     * DEC-051：计划偏差确认后更新合同基准完工日，并按阶段 defaultDays 相对权重重新展开阶段计划。
     *
     * <p>仅允许“计划偏差确认（Extend）”与“合同工期变更”调用；排期链路不得调用本方法。</p>
     */
    @Transactional
    public void updateBaselineEndDate(ProjectContract contract, LocalDate newEndDate) {
        if (contract == null || contract.getContractId() == null || newEndDate == null) {
            return;
        }
        ContractConstructionPlan plan = constructionPlanRepository
                .findByContract_ContractIdAndCompany_ComId(contract.getContractId(), contract.getCompany().getComId())
                .orElse(null);
        if (plan == null) {
            return;
        }
        LocalDate start = plan.getStartDate() == null ? contract.getStartDate() : plan.getStartDate();
        if (start == null || newEndDate.isBefore(start)) {
            return;
        }
        plan.setEndDate(newEndDate);
        constructionPlanRepository.save(plan);
        List<ContractConstructionStage> stages =
                constructionStageRepository.findByPlan_PlanIdOrderBySortAsc(plan.getPlanId());
        if (stages.isEmpty()) {
            return;
        }
        int totalTemplateDays = stages.stream()
                .mapToInt(item -> Math.max(1, item.getDefaultDays() == null ? 1 : item.getDefaultDays())).sum();
        long actualDays = Math.max(1, ChronoUnit.DAYS.between(start, newEndDate) + 1);
        LocalDate cursor = start;
        for (int index = 0; index < stages.size(); index++) {
            ContractConstructionStage stage = stages.get(index);
            int defaultDays = Math.max(1, stage.getDefaultDays() == null ? 1 : stage.getDefaultDays());
            int days = index == stages.size() - 1
                    ? Math.max(1, (int) ChronoUnit.DAYS.between(cursor, newEndDate) + 1)
                    : Math.max(1, BigDecimal.valueOf(actualDays)
                            .multiply(BigDecimal.valueOf(defaultDays))
                            .divide(BigDecimal.valueOf(totalTemplateDays), 0, RoundingMode.HALF_UP).intValue());
            days = Math.min(days, Math.max(1, (int) ChronoUnit.DAYS.between(cursor, newEndDate) + 1));
            LocalDate stageEnd = cursor.plusDays(days - 1L);
            stage.setPlannedStart(cursor);
            stage.setPlannedEnd(stageEnd);
            cursor = stageEnd.plusDays(1);
        }
        constructionStageRepository.saveAll(stages);
    }

    /** 签约价格确认后按最终签约价重算计划节点金额。 */
    @Transactional
    public void repricePaymentPlan(ProjectContract contract) {
        ContractPaymentPlan plan = paymentPlanRepository.findByContract_ContractIdAndCompany_ComId(
                contract.getContractId(), contract.getCompany().getComId()).orElse(null);
        if (plan == null) return;
        BigDecimal total = value(contract.getSignedAmount());
        plan.setTotalAmount(total);
        List<ContractPaymentNode> nodes = paymentNodeRepository.findByPlan_PlanIdOrderBySortAsc(plan.getPlanId());
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < nodes.size(); index++) {
            ContractPaymentNode node = nodes.get(index);
            BigDecimal amount = index == nodes.size() - 1
                    ? total.subtract(allocated)
                    : money(total.multiply(value(node.getRatio())));
            node.setAmount(amount);
            allocated = allocated.add(amount);
        }
        paymentNodeRepository.saveAll(nodes);
        paymentPlanRepository.save(plan);
    }

    private void createConstruction(ProjectContract contract, String code) {
        ConstructionPlanTemplate template = templateContext.resolveConstructionPlan(
                contract.getCompany().getComId(), code);
        LocalDate start = contract.getStartDate() == null ? LocalDate.now() : contract.getStartDate();
        LocalDate end = contract.getPlanEndDate() == null ? start.plusDays(Math.max(1, template.getTotalDays()) - 1) : contract.getPlanEndDate();
        if (end.isBefore(start)) end = start;

        ContractConstructionPlan plan = new ContractConstructionPlan();
        plan.setCompany(contract.getCompany());
        plan.setContract(contract);
        plan.setSourceCode(template.getCode());
        plan.setSourceVersion(template.getVersion());
        plan.setName(template.getName());
        plan.setStartDate(start);
        plan.setEndDate(end);
        plan.setStatus(PlanSnapshotStatus.Confirmed);
        plan = constructionPlanRepository.save(plan);

        List<ConstructionStageTemplate> templates = template.getStages().stream()
                .sorted(Comparator.comparing(ConstructionStageTemplate::getSort)).toList();
        int totalTemplateDays = templates.stream().mapToInt(item -> Math.max(1, item.getDefaultDays())).sum();
        long actualDays = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);
        LocalDate cursor = start;
        for (int index = 0; index < templates.size(); index++) {
            ConstructionStageTemplate item = templates.get(index);
            int days = index == templates.size() - 1
                    ? Math.max(1, (int) ChronoUnit.DAYS.between(cursor, end) + 1)
                    : Math.max(1, BigDecimal.valueOf(actualDays)
                            .multiply(BigDecimal.valueOf(item.getDefaultDays()))
                            .divide(BigDecimal.valueOf(totalTemplateDays), 0, RoundingMode.HALF_UP).intValue());
            days = Math.min(days, Math.max(1, (int) ChronoUnit.DAYS.between(cursor, end) + 1));
            LocalDate stageEnd = cursor.plusDays(days - 1L);
            ContractConstructionStage stage = new ContractConstructionStage();
            stage.setCompany(contract.getCompany());
            stage.setPlan(plan);
            stage.setCode(item.getCode());
            stage.setName(item.getName());
            stage.setSort(item.getSort());
            stage.setDefaultDays(item.getDefaultDays());
            stage.setPlannedStart(cursor);
            stage.setPlannedEnd(stageEnd);
            stage.setAcceptanceMode(item.getAcceptanceMode());
            stage.setDescription(item.getDescription());
            constructionStageRepository.save(stage);
            cursor = stageEnd.plusDays(1);
        }
    }

    private void createPayment(ProjectContract contract, String code) {
        PaymentPlanTemplate template = templateContext.resolvePaymentPlan(
                contract.getCompany().getComId(), code);
        if (contract.getPaymentTerms() == null || contract.getPaymentTerms().isBlank()) {
            contract.setPaymentTerms(template.getNodes().stream()
                    .sorted(Comparator.comparing(PaymentPlanNodeTemplate::getSort))
                    .map(item -> item.getName() + money(item.getRatio().multiply(BigDecimal.valueOf(100)))
                            .stripTrailingZeros().toPlainString() + "%")
                    .collect(java.util.stream.Collectors.joining(" / ")));
        }
        ContractPaymentPlan plan = new ContractPaymentPlan();
        plan.setCompany(contract.getCompany());
        plan.setContract(contract);
        plan.setSourceCode(template.getCode());
        plan.setSourceVersion(template.getVersion());
        plan.setName(template.getName());
        plan.setTotalAmount(value(contract.getCurrentAmount()));
        plan.setStatus(PlanSnapshotStatus.Confirmed);
        plan = paymentPlanRepository.save(plan);

        ContractConstructionPlan construction = constructionPlan(contract);
        Map<String, ContractConstructionStage> stages = new HashMap<>();
        if (construction != null) {
            for (ContractConstructionStage stage : construction.getStages()) {
                stages.put(stage.getCode(), stage);
            }
        }
        List<PaymentPlanNodeTemplate> templates = template.getNodes().stream()
                .sorted(Comparator.comparing(PaymentPlanNodeTemplate::getSort)).toList();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < templates.size(); index++) {
            PaymentPlanNodeTemplate item = templates.get(index);
            BigDecimal amount = index == templates.size() - 1
                    ? value(contract.getCurrentAmount()).subtract(allocated)
                    : money(value(contract.getCurrentAmount()).multiply(item.getRatio()));
            ContractPaymentNode node = new ContractPaymentNode();
            node.setCompany(contract.getCompany());
            node.setPlan(plan);
            node.setCode(item.getCode());
            node.setName(item.getName());
            node.setRatio(item.getRatio());
            node.setAmount(amount);
            node.setType(item.getType());
            node.setTriggerType(item.getTriggerType());
            node.setTriggerStageCode(item.getTriggerStageCode());
            ContractConstructionStage triggerStage = item.getTriggerStageCode() == null
                    ? null : stages.get(item.getTriggerStageCode());
            node.setTriggerStageName(triggerStage == null ? null : triggerStage.getName());
            node.setDueDate(dueDate(contract, node, triggerStage));
            node.setOffsetDays(item.getOffsetDays() == null ? 0 : item.getOffsetDays());
            node.setSort(item.getSort());
            paymentNodeRepository.save(node);
            allocated = allocated.add(amount);
        }
    }

    private LocalDate dueDate(ProjectContract contract, ContractPaymentNode node,
                              ContractConstructionStage triggerStage) {
        LocalDate base;
        if (node.getTriggerType() == PaymentTriggerType.ContractSigned) {
            base = contract.getSignedAt() == null ? LocalDate.now() : contract.getSignedAt();
        } else if (node.getTriggerType() == PaymentTriggerType.ProjectFinished) {
            base = contract.getPlanEndDate() == null ? LocalDate.now().plusDays(90) : contract.getPlanEndDate();
        } else if (triggerStage == null) {
            base = contract.getStartDate() == null ? LocalDate.now() : contract.getStartDate();
        } else if (node.getTriggerType() == PaymentTriggerType.StageStarted) {
            base = triggerStage.getPlannedStart();
        } else {
            base = triggerStage.getPlannedEnd();
        }
        return base.plusDays(node.getOffsetDays() == null ? 0 : node.getOffsetDays());
    }

    private String planCode(String code) {
        return code == null || code.isBlank() ? null : code;
    }

    private BigDecimal money(BigDecimal value) {
        return value(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }
}
