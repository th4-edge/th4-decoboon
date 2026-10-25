package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.common.configure.FactStage;
import com.th4.edge.decoboon.common.configure.FactType;
import com.th4.edge.decoboon.common.configure.FactVisibility;
import com.th4.edge.decoboon.common.model.BusinessFactContract;
import com.th4.edge.decoboon.common.model.BusinessFactNode;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.decoboon.property.repository.MeasureRegionRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractWorkbench;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 合同履约档案聚合上下文。
 *
 * <p>把单份合同的金额、回款、成本利润、付款计划节点状态、工地进度、变更汇总与
 * 主材选材状态聚合成一个只读视图。所有数字来自真实领域事实，不新增业务裁决。</p>
 */
@Component
public class ContractWorkbenchContext {

    private static final int SELECTION_SUMMARY_LENGTH = 120;

    private final ProjectContractContext contractContext;
    private final ReceivableRepository receivableRepository;
    private final com.th4.edge.decoboon.plan.repository.ContractPaymentNodeRepository paymentNodeRepository;
    private final ProjectCostContext costContext;
    private final ProjectStageRepository stageRepository;
    private final ContractChangeRepository changeRepository;
    private final MaterialSelectionRepository selectionRepository;
    private final MeasureRepository measureRepository;
    private final MeasureRegionRepository measureRegionRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final AcceptanceRepository acceptanceRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.th4.edge.decoboon.finance.context.ManualLedgerContext manualLedgerContext;

    public ContractWorkbenchContext(com.th4.edge.decoboon.plan.repository.ContractPaymentNodeRepository paymentNodeRepository,
                                    ProjectContractContext contractContext,
                                    ReceivableRepository receivableRepository,
                                    ProjectCostContext costContext,
                                    ProjectStageRepository stageRepository,
                                    ContractChangeRepository changeRepository,
                                    MaterialSelectionRepository selectionRepository,
                                    MeasureRepository measureRepository,
                                    MeasureRegionRepository measureRegionRepository,
                                    MaterialPlanRepository materialPlanRepository,
                                    AcceptanceRepository acceptanceRepository) {
        this.contractContext = contractContext;
        this.receivableRepository = receivableRepository;
        this.paymentNodeRepository = paymentNodeRepository;
        this.costContext = costContext;
        this.stageRepository = stageRepository;
        this.changeRepository = changeRepository;
        this.selectionRepository = selectionRepository;
        this.measureRepository = measureRepository;
        this.measureRegionRepository = measureRegionRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.acceptanceRepository = acceptanceRepository;
    }

    @Transactional(readOnly = true)
    public ContractWorkbench build(ProjectContract contract) {
        return build(contract, true);
    }

    @Transactional(readOnly = true)
    public ContractWorkbench build(ProjectContract contract, boolean includeInternalCost) {
        ProjectContract enriched = contractContext.enrich(contract);
        ContractWorkbench workbench = new ContractWorkbench();
        workbench.setContract(enriched);

        Project project = enriched.getProject();
        List<Receivable> receivables = project == null
                ? List.of()
                : receivableRepository.findByProjectOrderByDueDateAsc(project);

        workbench.setReceivable(receivable(receivables));
        workbench.setPaymentNodes(paymentNodes(enriched.getPaymentPlan(), receivables));
        workbench.setCost(includeInternalCost ? cost(enriched) : null);
        workbench.setProgress(progress(project));
        workbench.setChange(change(enriched));
        workbench.setSelection(selection(enriched));
        ContractFacts facts = facts(enriched, workbench, includeInternalCost);
        workbench.setUpstreamFacts(facts.upstream());
        workbench.setDownstreamFacts(facts.downstream());
        workbench.setSourceGaps(facts.gaps());
        return workbench;
    }

    private ContractFacts facts(ProjectContract contract, ContractWorkbench workbench, boolean includeInternalCost) {
        List<BusinessFactNode> upstream = new ArrayList<>();
        List<BusinessFactNode> downstream = new ArrayList<>();
        List<String> gaps = new ArrayList<>();

        Long projectId = contract.getProject() == null ? null : contract.getProject().getProjectId();
        if (contract.getCustomer() != null) {
            upstream.add(node(FactType.Customer, "Customer", contract.getCustomer().getCustId(), 1, projectId,
                    null, null, auditTime(contract.getCustomer().getCreateTime()), null,
                    contract.getCustomer().getName(), "合同客户", null, "customer:" + contract.getCustomer().getCustId()));
        }
        if (contract.getProperty() != null) {
            upstream.add(node(FactType.Property, "Property", contract.getProperty().getPropertyId(), 1, projectId,
                    null, null, auditTime(contract.getProperty().getCreateTime()), null,
                    contract.getProperty().getCommunity(), summary(contract.getProperty().getAddress()), null,
                    "property:" + contract.getProperty().getPropertyId()));
        }

        Measure measure = latestCompletedMeasure(contract);
        if (measure == null) {
            gaps.add("无系统量房记录");
        } else {
            long regionCount = 0;
            long measuredCount = 0;
            if (contract.getCompany() != null) {
                List<MeasureRegion> regions = measureRegionRepository
                        .findByCompany_ComIdAndMeasure_MeasureIdOrderBySortAscRegionIdAsc(
                                contract.getCompany().getComId(), measure.getMeasureId());
                regionCount = regions.size();
                measuredCount = regions.stream()
                        .filter(region -> region.getAreaSource() == AreaSource.Measured && region.getArea() != null)
                        .count();
            }
            String actor = measure.getExecutor() != null ? measure.getExecutor().getName()
                    : measure.getDesigner() == null ? null : measure.getDesigner().getName();
            upstream.add(node(FactType.Measure, "Measure", measure.getMeasureId(), 1, projectId,
                    measure.getStatus().name(), measure.getStatus().getName(), measure.getCompletedAt(), actor,
                    "量房记录", "区域 " + regionCount + " · 实测 " + measuredCount + " · 待补 " + (regionCount - measuredCount),
                    null, "measure:" + measure.getMeasureId()));
        }

        Quote quote = contract.getQuote();
        Design design = quote == null ? null : quote.getDesign();
        if (design == null) {
            gaps.add("无设计方案来源");
        } else {
            upstream.add(node(FactType.Design, "Design", design.getDesignId(), design.getVersion(), projectId,
                    design.getStatus() == null ? null : design.getStatus().name(),
                    design.getStatus() == null ? null : design.getStatus().getName(),
                    auditTime(design.getCreateTime()), design.getOwner() == null ? null : design.getOwner().getName(),
                    design.getName(), "绑定设计方案", null, "design:" + design.getDesignId()));
        }
        if (quote != null) {
            upstream.add(node(FactType.Quote, "Quote", quote.getQuoteId(), quote.getVersion(), projectId,
                    quote.getStatus() == null ? null : quote.getStatus().name(),
                    quote.getStatus() == null ? null : quote.getStatus().getName(),
                    quote.getAcceptedAt() == null ? quote.getSubmittedAt() : quote.getAcceptedAt(),
                    quote.getAcceptedBy() == null ? null : quote.getAcceptedBy().getName(),
                    quote.getNo(), "签约报价基线 V" + value(quote.getVersion()), quote.getAmount(),
                    "quote:" + quote.getQuoteId()));
        }
        upstream.add(node(FactType.Contract, "Contract", contract.getContractId(), 1, projectId,
                contract.getStatus() == null ? null : contract.getStatus().name(),
                contract.getStatus() == null ? null : contract.getStatus().getName(),
                contract.getSignedAt() == null ? null : contract.getSignedAt().atStartOfDay(),
                contract.getCreatedBy() == null ? null : contract.getCreatedBy().getName(),
                contract.getNo(), contract.getBusinessOrigin() == BusinessOrigin.Carryover
                        ? BusinessFactContract.CARRYOVER_SOURCE : "正常业务",
                contract.getCurrentAmount(), "contract:" + contract.getContractId()));

        if (contract.getProject() != null) {
            downstream.add(node(FactType.Project, "Project", contract.getProject().getProjectId(), 1, projectId,
                    contract.getProject().getStatus() == null ? null : contract.getProject().getStatus().name(),
                    contract.getProject().getStatus() == null ? null : contract.getProject().getStatus().getName(),
                    auditTime(contract.getProject().getCreateTime()),
                    contract.getProject().getManager() == null ? null : contract.getProject().getManager().getName(),
                    contract.getProject().getName(), "项目当前状态", contract.getProject().getContractAmount(),
                    "project:" + projectId));
        }
        if (workbench.getChange() != null && value(workbench.getChange().getTotalCount()) > 0) {
            downstream.add(node(FactType.ContractChange, "ContractChangeAggregate", contract.getContractId(), 1, projectId,
                    null, null, null, null, "合同增减项",
                    "共 " + workbench.getChange().getTotalCount() + " 单 · 待审批 " + workbench.getChange().getPendingCount(),
                    workbench.getChange().getConfirmedAmount(), "contract-change:" + contract.getContractId()));
        }
        if (contract.getProject() != null) {
            long materialCount = materialPlanRepository.countByProject(contract.getProject());
            if (materialCount > 0) {
                downstream.add(node(FactType.MaterialPlan, "MaterialPlanAggregate", projectId, 1, projectId,
                        null, null, null, null, "材料计划", materialCount + " 条材料计划", null,
                        "project:" + projectId + ":materials"));
            }
            long acceptanceCount = acceptanceRepository.countByProject_ProjectId(projectId);
            if (acceptanceCount > 0) {
                downstream.add(node(FactType.Acceptance, "AcceptanceAggregate", projectId, 1, projectId,
                        null, null, null, null, "阶段验收", acceptanceCount + " 条验收事实", null,
                        "project:" + projectId + ":acceptance"));
            }
        }
        if (workbench.getReceivable() != null && value(workbench.getReceivable().getTotalCount()) > 0) {
            downstream.add(node(FactType.Receivable, "ReceivableAggregate", contract.getContractId(), 1, projectId,
                    null, null, null, null, "应收与回款",
                    "应收 " + money(workbench.getReceivable().getReceivableTotal())
                            + " · 已收 " + money(workbench.getReceivable().getReceivedTotal())
                            + " · 逾期 " + money(workbench.getReceivable().getOverdueTotal()),
                    workbench.getReceivable().getOutstandingTotal(), "contract:" + contract.getContractId() + ":receivables"));
        }
        if (includeInternalCost && contract.getProject() != null) {
            downstream.add(node(FactType.Cost, "ProjectCost", projectId, 1, projectId,
                    Boolean.TRUE.equals(contract.getCostComplete()) ? "Complete" : "Incomplete",
                    Boolean.TRUE.equals(contract.getCostComplete()) ? "成本完整" : "成本未完整",
                    null, null, "项目成本", "签约成本快照", contract.getSignedCost(),
                    "project:" + projectId + ":cost"));
            downstream.get(downstream.size() - 1).setVisibility(FactVisibility.Internal);
        }

        upstream.sort(java.util.Comparator
                .comparingInt((BusinessFactNode node) -> contractUpstreamOrder(node.getFactType()))
                .thenComparing(BusinessFactContract.stableOrder()));
        downstream.sort(BusinessFactContract.stableOrder());
        return new ContractFacts(List.copyOf(upstream), List.copyOf(downstream), List.copyOf(gaps));
    }

    private int contractUpstreamOrder(FactType type) {
        return switch (type) {
            case Customer -> 10;
            case Property -> 20;
            case Measure -> 30;
            case Design -> 40;
            case Quote -> 50;
            case Contract -> 60;
            default -> 100;
        };
    }

    private Measure latestCompletedMeasure(ProjectContract contract) {
        if (contract.getCompany() == null || contract.getProperty() == null) return null;
        return measureRepository.findByCompany_ComIdAndProperty_PropertyIdOrderByMeasureTimeDesc(
                        contract.getCompany().getComId(), contract.getProperty().getPropertyId()).stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Completed)
                .sorted(java.util.Comparator
                        .comparing(Measure::getCompletedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(Measure::getMeasureId, java.util.Comparator.nullsLast(Long::compareTo).reversed()))
                .findFirst().orElse(null);
    }

    private BusinessFactNode node(FactType type, String objectType, Long objectId, Integer version, Long projectId,
                                  String status, String statusName, LocalDateTime businessTime, String actor,
                                  String title, String summary, BigDecimal amount, String navigationPath) {
        BusinessFactNode node = new BusinessFactNode();
        node.setNodeId(type.name() + "-" + objectType + "-" + objectId);
        node.setNodeKey(type.name() + ":" + objectType + ":" + objectId + ":" + version);
        node.setStage(type.getStage());
        node.setFactType(type);
        node.setObjectType(objectType);
        node.setObjectId(objectId);
        node.setVersion(version);
        node.setProjectId(projectId);
        node.setCurrent(true);
        node.setStatus(status);
        node.setStatusName(statusName);
        node.setBusinessTime(businessTime);
        node.setRecordedAt(LocalDateTime.now());
        node.setActorName(actor);
        node.setTitle(title);
        node.setSummary(summary);
        node.setAmount(amount);
        node.setVisibility(FactVisibility.OwnerVisible);
        node.setNavigationPath(navigationPath);
        node.setReturnPath("contract:" + (objectId == null ? "" : objectId));
        node.setReturnAnchor("contract-facts");
        node.setSourceLabel("合同关联事实");
        if (projectId == null) {
            node.setUncertainty("项目尚未建立，事实按合同绑定关系展示");
        }
        return node;
    }

    private long value(Integer value) {
        return value == null ? 0 : value;
    }

    private LocalDateTime auditTime(DateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private record ContractFacts(List<BusinessFactNode> upstream, List<BusinessFactNode> downstream, List<String> gaps) {}

    private ContractWorkbench.Receivable receivable(List<Receivable> receivables) {
        ContractWorkbench.Receivable summary = new ContractWorkbench.Receivable();
        LocalDate today = LocalDate.now();
        BigDecimal receivableTotal = BigDecimal.ZERO;
        BigDecimal receivedTotal = BigDecimal.ZERO;
        BigDecimal outstandingTotal = BigDecimal.ZERO;
        BigDecimal overdueTotal = BigDecimal.ZERO;
        int overdueCount = 0;
        for (Receivable item : receivables) {
            BigDecimal amount = value(item.getAmount());
            BigDecimal received = value(item.getReceivedAmount());
            BigDecimal outstanding = amount.subtract(received);
            if (outstanding.signum() < 0) {
                outstanding = BigDecimal.ZERO;
            }
            receivableTotal = receivableTotal.add(amount);
            receivedTotal = receivedTotal.add(received);
            outstandingTotal = outstandingTotal.add(outstanding);
            if (isOverdue(item, today) && outstanding.signum() > 0) {
                overdueCount++;
                overdueTotal = overdueTotal.add(outstanding);
            }
        }
        summary.setReceivableTotal(money(receivableTotal));
        summary.setReceivedTotal(money(receivedTotal));
        summary.setOutstandingTotal(money(outstandingTotal));
        summary.setOverdueTotal(money(overdueTotal));
        summary.setOverdueCount(overdueCount);
        summary.setTotalCount(receivables.size());
        return summary;
    }

    private List<ContractWorkbench.PaymentNode> paymentNodes(ContractPaymentPlan plan,
                                                            List<Receivable> receivables) {
        if (plan == null || plan.getNodes() == null) {
            return List.of();
        }
        // 按应收集合一次取回付款节点（替代原 Receivable 反向 OneToOne 的逐条懒加载，后者每条应收都会打一次库）。
        Map<Long, Receivable> byNode = new HashMap<>();
        List<Long> receivableIds = receivables.stream()
                .map(Receivable::getReceivableId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (!receivableIds.isEmpty()) {
            for (ContractPaymentNode node : paymentNodeRepository.findByReceivable_ReceivableIdIn(receivableIds)) {
                if (node != null && node.getNodeId() != null && node.getReceivable() != null) {
                    byNode.putIfAbsent(node.getNodeId(), node.getReceivable());
                }
            }
        }
        List<ContractWorkbench.PaymentNode> nodes = new ArrayList<>();
        for (ContractPaymentNode node : plan.getNodes()) {
            ContractWorkbench.PaymentNode view = new ContractWorkbench.PaymentNode();
            view.setNodeId(node.getNodeId());
            view.setCode(node.getCode());
            view.setName(node.getName());
            view.setRatio(node.getRatio());
            BigDecimal planned = money(value(node.getAmount()));
            view.setAmount(planned);
            view.setTriggerType(node.getTriggerType() == null ? null : node.getTriggerType().name());
            view.setTriggerStageName(node.getTriggerStageName());
            view.setDueDate(node.getDueDate());
            Receivable receivable = node.getNodeId() == null ? null : byNode.get(node.getNodeId());
            if (receivable == null) {
                view.setReceivedAmount(BigDecimal.ZERO);
                view.setOutstandingAmount(planned);
                view.setStatus("pending");
                view.setStatusName("待生成应收");
            } else {
                BigDecimal received = money(value(receivable.getReceivedAmount()));
                BigDecimal outstanding = planned.subtract(received);
                if (outstanding.signum() < 0) {
                    outstanding = BigDecimal.ZERO;
                }
                view.setReceivableId(receivable.getReceivableId());
                view.setReceivedAmount(received);
                view.setOutstandingAmount(money(outstanding));
                view.setDueDate(receivable.getDueDate() == null ? node.getDueDate() : receivable.getDueDate());
                ReceivableStatus status = receivable.getStatus();
                view.setStatus(status == null ? null : status.name());
                view.setStatusName(isOverdue(receivable, LocalDate.now()) && outstanding.signum() > 0
                        ? "已逾期"
                        : status == null ? null : status.getName());
            }
            nodes.add(view);
        }
        return nodes;
    }

    private ContractWorkbench.Cost cost(ProjectContract contract) {
        ContractWorkbench.Cost view = new ContractWorkbench.Cost();
        ProjectCostContext.CostSummary summary = costContext.summarize(contract.getProject());
        com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts manual = manualAmounts(contract.getProject());
        view.setEstimatedCost(money(summary.estimatedCost()));
        view.setActualCost(money(summary.actualCost()));
        view.setCostComplete(summary.complete());
        view.setIncompleteItemCount(summary.incompleteItemCount());
        if (summary.complete()) {
            BigDecimal income = value(contract.getCurrentAmount()).add(manual.income());
            BigDecimal profit = income.subtract(value(summary.actualCost()));
            view.setGrossProfit(money(profit));
            view.setGrossMargin(income.signum() == 0
                    ? null
                    : profit.divide(income, 4, RoundingMode.HALF_UP));
        }
        return view;
    }

    private com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts manualAmounts(Project project) {
        return manualLedgerContext == null
                ? com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts.ZERO
                : manualLedgerContext.amounts(project);
    }

    private ContractWorkbench.Progress progress(Project project) {
        ContractWorkbench.Progress view = new ContractWorkbench.Progress();
        if (project == null) {
            return view;
        }
        view.setProjectId(project.getProjectId());
        view.setProjectNo(project.getNo());
        view.setProjectName(project.getName());
        view.setStatus(project.getStatus() == null ? null : project.getStatus().name());
        view.setStatusName(project.getStatus() == null ? null : project.getStatus().getName());
        view.setPercent(project.getProgress() == null ? 0 : project.getProgress());
        view.setCurrentStageName(project.getCurrentStageName());
        view.setStartDate(project.getStartDate());
        view.setPlanEndDate(project.getPlanEndDate());
        view.setExpectedEndDate(project.getExpectedEndDate());
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        view.setStageTotal(stages.size());
        view.setStageDone((int) stages.stream()
                .filter(stage -> stage.getStatus() == StageStatus.Done).count());
        view.setPendingAcceptanceCount((int) stages.stream()
                .filter(stage -> stage.getStatus() == StageStatus.Accepting).count());
        return view;
    }

    private ContractWorkbench.Change change(ProjectContract contract) {
        ContractWorkbench.Change view = new ContractWorkbench.Change();
        List<ContractChange> changes = changeRepository.findByContractOrderByChangeIdDesc(contract);
        view.setTotalCount(changes.size());
        int pending = 0;
        int approved = 0;
        int confirmed = 0;
        int closed = 0;
        BigDecimal confirmedAmount = BigDecimal.ZERO;
        for (ContractChange item : changes) {
            ContractChangeStatus status = item.getStatus();
            if (status == ContractChangeStatus.Pending) {
                pending++;
            } else if (status == ContractChangeStatus.Approved) {
                approved++;
            } else if (status == ContractChangeStatus.CustomerConfirmed) {
                confirmed++;
                confirmedAmount = confirmedAmount.add(value(item.getAmount()));
            } else {
                closed++;
            }
        }
        view.setPendingCount(pending);
        view.setApprovedCount(approved);
        view.setConfirmedCount(confirmed);
        view.setClosedCount(closed);
        view.setConfirmedAmount(money(confirmedAmount));
        return view;
    }

    private ContractWorkbench.Selection selection(ProjectContract contract) {
        ContractWorkbench.Selection view = new ContractWorkbench.Selection();
        if (contract.getProperty() == null || contract.getProperty().getPropertyId() == null) {
            return view;
        }
        Long comId = contract.getCompany().getComId();
        Long propertyId = contract.getProperty().getPropertyId();
        List<MaterialSelection> records = contract.getContractId() == null
                ? selectionRepository.findByCompany_ComIdAndProperty_PropertyIdOrderByVersionDesc(comId, propertyId)
                : selectionRepository.findByCompany_ComIdAndProperty_PropertyIdOrderByVersionDesc(comId, propertyId)
                        .stream()
                        .filter(item -> item.getContract() != null
                                && contract.getContractId().equals(item.getContract().getContractId()))
                        .toList();
        view.setTotalCount(records.size());
        if (records.isEmpty()) {
            return view;
        }
        MaterialSelection latest = records.get(0);
        view.setSelectionId(latest.getSelectionId());
        view.setVersion(latest.getVersion());
        view.setStatus(latest.getStatus() == null ? null : latest.getStatus().name());
        view.setStatusName(latest.getStatus() == null ? null : latest.getStatus().getName());
        view.setTitle(latest.getTitle());
        view.setSummary(summary(latest.getContent()));
        view.setDesignerName(latest.getDesigner() == null ? null : latest.getDesigner().getName());
        return view;
    }

    private String summary(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > SELECTION_SUMMARY_LENGTH
                ? normalized.substring(0, SELECTION_SUMMARY_LENGTH) + "…"
                : normalized;
    }

    private boolean isOverdue(Receivable receivable, LocalDate today) {
        if (receivable == null) {
            return false;
        }
        ReceivableStatus status = receivable.getStatus();
        if (status == ReceivableStatus.Received || status == ReceivableStatus.Closed) {
            return false;
        }
        if (status == ReceivableStatus.Overdue) {
            return true;
        }
        return receivable.getDueDate() != null && receivable.getDueDate().isBefore(today);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value(value).setScale(2, RoundingMode.HALF_UP);
    }
}
