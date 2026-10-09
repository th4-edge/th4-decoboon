package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractWorkbench;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.th4.edge.decoboon.finance.context.ManualLedgerContext manualLedgerContext;

    public ContractWorkbenchContext(com.th4.edge.decoboon.plan.repository.ContractPaymentNodeRepository paymentNodeRepository,
                                    ProjectContractContext contractContext,
                                    ReceivableRepository receivableRepository,
                                    ProjectCostContext costContext,
                                    ProjectStageRepository stageRepository,
                                    ContractChangeRepository changeRepository,
                                    MaterialSelectionRepository selectionRepository) {
        this.contractContext = contractContext;
        this.receivableRepository = receivableRepository;
        this.paymentNodeRepository = paymentNodeRepository;
        this.costContext = costContext;
        this.stageRepository = stageRepository;
        this.changeRepository = changeRepository;
        this.selectionRepository = selectionRepository;
    }

    @Transactional(readOnly = true)
    public ContractWorkbench build(ProjectContract contract) {
        ProjectContract enriched = contractContext.enrich(contract);
        ContractWorkbench workbench = new ContractWorkbench();
        workbench.setContract(enriched);

        Project project = enriched.getProject();
        List<Receivable> receivables = project == null
                ? List.of()
                : receivableRepository.findByProjectOrderByDueDateAsc(project);

        workbench.setReceivable(receivable(receivables));
        workbench.setPaymentNodes(paymentNodes(enriched.getPaymentPlan(), receivables));
        workbench.setCost(cost(enriched));
        workbench.setProgress(progress(project));
        workbench.setChange(change(enriched));
        workbench.setSelection(selection(enriched));
        return workbench;
    }

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
        view.setActualCost(money(value(summary.actualCost()).add(manual.cost())));
        view.setCostComplete(summary.complete());
        view.setIncompleteItemCount(summary.incompleteItemCount());
        if (summary.complete()) {
            BigDecimal income = value(contract.getCurrentAmount()).add(manual.income());
            BigDecimal profit = income.subtract(value(summary.actualCost()).add(manual.cost()));
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
