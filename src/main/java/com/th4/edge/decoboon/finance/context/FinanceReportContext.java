package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.carryover.context.CarryoverCostContext;
import com.th4.edge.decoboon.common.context.BusinessOriginFilter;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.project.configure.ProjectArchivePolicy;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Map;
import java.util.Set;
import com.th4.edge.decoboon.common.context.StoreScope;

/** 财务汇总、项目财务全景与结算快照计算。 */
@Component
public class FinanceReportContext {

    private final FinanceAccessContext accessContext;
    private final ProjectRepository projectRepository;
    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;
    private final LedgerRepository ledgerRepository;
    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final ProjectCostContext projectCostContext;
    private final CarryoverCostContext carryoverCostContext;
    private final ManualLedgerContext manualLedgerContext;
    private final OpeningBalanceContext openingBalanceContext;

    public FinanceReportContext(FinanceAccessContext accessContext,
                                ProjectRepository projectRepository,
                                ReceivableRepository receivableRepository,
                                PayableRepository payableRepository,
                                LedgerRepository ledgerRepository,
                                SettlementRepository settlementRepository,
                                ProjectCostContext projectCostContext) {
        this(accessContext, projectRepository, receivableRepository, payableRepository, ledgerRepository,
                null, null, settlementRepository, projectCostContext, null,
                new ManualLedgerContext(ledgerRepository), null);
    }

    @Autowired
    public FinanceReportContext(FinanceAccessContext accessContext,
                                ProjectRepository projectRepository,
                                ReceivableRepository receivableRepository,
                                PayableRepository payableRepository,
                                LedgerRepository ledgerRepository,
                                ReceiptRepository receiptRepository,
                                PaymentRepository paymentRepository,
                                SettlementRepository settlementRepository,
                                ProjectCostContext projectCostContext,
                                CarryoverCostContext carryoverCostContext,
                                ManualLedgerContext manualLedgerContext,
                                OpeningBalanceContext openingBalanceContext) {
        this.accessContext = accessContext;
        this.projectRepository = projectRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
        this.ledgerRepository = ledgerRepository;
        this.receiptRepository = receiptRepository;
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.projectCostContext = projectCostContext;
        this.carryoverCostContext = carryoverCostContext;
        this.manualLedgerContext = manualLedgerContext;
        this.openingBalanceContext = openingBalanceContext;
    }

    /** 企业范围全量汇总（沿用旧签名：BOSS/总部财务视角，不限门店）。 */
    public FinanceSummary summary(Employee principal) {
        return summary(principal, null);
    }

    /**
     * 财务工作台汇总（DEC-065 门店维度）。
     *
     * <p>门店范围主体只能用本店；企业范围主体缺省为全部门店，可按 {@code storeId} 下钻单店。</p>
     */
    public FinanceSummary summary(Employee principal, Long requestedStoreId) {
        Employee current = accessContext.requireReader(principal);
        Long storeFilter = accessContext.resolveStoreFilter(current, requestedStoreId);
        List<Project> allProjects = projects(current, storeFilter).stream()
                .filter(project -> StoreScope.inStore(StoreScope.storeIdOf(project), storeFilter))
                .toList();
        Set<Long> historicalArchiveProjectIds = allProjects.stream()
                .filter(ProjectArchivePolicy::isHistoricalArchive)
                .map(Project::getProjectId)
                .collect(java.util.stream.Collectors.toSet());
        List<Project> projects = allProjects.stream()
                .filter(project -> !ProjectArchivePolicy.isHistoricalArchive(project))
                .toList();
        List<Receivable> receivables = receivableRepository.findAll(accessContext.visible(current, Receivable.class)).stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .filter(item -> !belongsToArchivedProject(item.getProject(), historicalArchiveProjectIds))
                .toList();
        List<Payable> payables = payableRepository.findAll(accessContext.visible(current, Payable.class)).stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .filter(item -> !belongsToArchivedProject(item.getProject(), historicalArchiveProjectIds))
                .toList();
        List<Ledger> ledgers = ledgerRepository.findAll(accessContext.visible(current, Ledger.class)).stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .filter(item -> !Boolean.TRUE.equals(item.getVoided()))
                .toList();
        List<Receipt> receipts = receiptRepository == null ? List.of()
                : receiptRepository.findAll(accessContext.visible(current, Receipt.class)).stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .toList();
        List<Payment> payments = paymentRepository == null ? List.of()
                : paymentRepository.findAll(accessContext.visible(current, Payment.class)).stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .toList();

        FinanceSummary summary = new FinanceSummary();
        summary.setProjectCount(projects.size());
        summary.setActiveProjectCount((int) projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.Preparing
                        || project.getStatus() == ProjectStatus.Ongoing
                        || project.getStatus() == ProjectStatus.Accepting)
                .count());
        summary.setContractAmount(sum(projects.stream().map(Project::getContractAmount)));
        summary.setReceivableAmount(sum(receivables.stream().map(Receivable::getAmount)));
        summary.setReceivableCount(receivables.size());
        summary.setReceivedAmount(sum(receivables.stream().map(Receivable::getReceivedAmount)));
        summary.setOutstandingAmount(receivables.stream()
                .map(item -> accessContext.outstanding(item.getAmount(), item.getReceivedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setOverdueAmount(receivables.stream()
                .filter(this::receivableOverdue)
                .map(item -> accessContext.outstanding(item.getAmount(), item.getReceivedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setPayableAmount(sum(payables.stream().map(Payable::getAmount)));
        summary.setPaidAmount(sum(payables.stream().map(Payable::getPaidAmount)));
        summary.setUnpaidAmount(payables.stream()
                .map(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setMaterialPayableAmount(payableAmount(payables, PayableType.Material));
        summary.setSalaryPayableAmount(payableAmount(payables, PayableType.Salary));
        summary.setProjectPayableAmount(payableAmount(payables, PayableType.Project));
        summary.setOtherPayableAmount(sum(payables.stream()
                .filter(item -> item.getType() != PayableType.Material
                        && item.getType() != PayableType.Salary
                        && item.getType() != PayableType.Project)
                .map(Payable::getAmount)));

        YearMonth month = YearMonth.now();
        summary.setMonthIncome(sum(ledgers.stream()
                .filter(item -> item.getDirection() == LedgerDirection.Income && sameMonth(item.getOccurredAt(), month))
                .map(Ledger::getAmount)));
        summary.setMonthExpense(sum(ledgers.stream()
                .filter(item -> item.getDirection() == LedgerDirection.Expense && sameMonth(item.getOccurredAt(), month))
                .map(Ledger::getAmount)));
        summary.setMonthNetCashFlowAmount(summary.getMonthIncome().subtract(summary.getMonthExpense()));
        summary.setReceiptCount(receipts.size());
        summary.setPaymentCount(payments.size());
        summary.setReceiptAmount(sum(receipts.stream().map(Receipt::getAmount)));
        summary.setPaymentAmount(sum(payments.stream().map(Payment::getAmount)));
        summary.setMonthReceiptAmount(sum(receipts.stream()
                .filter(item -> sameMonth(item.getReceivedAt(), month))
                .map(Receipt::getAmount)));
        summary.setMonthPaymentAmount(sum(payments.stream()
                .filter(item -> sameMonth(item.getPaidAt(), month))
                .map(Payment::getAmount)));
        summary.setUrgentPayableAmount(payables.stream()
                .filter(item -> item.getStatus() == PayableStatus.Overdue || Boolean.TRUE.equals(item.getUrgent()))
                .map(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal openingBalance = openingBalanceContext == null
                ? BigDecimal.ZERO : openingBalanceContext.totalAtLatest(current.getCompany().getComId());
        LocalDate openingDate = openingBalanceContext == null
                ? null : openingBalanceContext.latestAsOfDate(current.getCompany().getComId());
        BigDecimal ledgerNet = ledgers.stream()
                .filter(item -> openingDate == null
                        || (item.getOccurredAt() != null && !item.getOccurredAt().isBefore(openingDate)))
                .map(item -> item.getDirection() == LedgerDirection.Income
                        ? accessContext.value(item.getAmount()) : accessContext.value(item.getAmount()).negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setOpeningBalanceAmount(openingBalance);
        summary.setOpeningBalanceCaptured(openingDate != null);
        summary.setCashBalance(openingBalance.add(ledgerNet));
        // 汇总与项目行、结算快照统一使用 ProjectCostContext + 同一 totalCost 公式，
        // 避免"汇总成本未完整、项目行却有利润"的口径冲突。
        Map<Long, BigDecimal> payableCostByProject = new HashMap<>();
        for (Payable payable : payables) {
            if (payable.getProject() == null || payable.getProject().getProjectId() == null
                    || !BusinessOriginFilter.isNormal(payable.getBusinessOrigin())) {
                continue;
            }
            payableCostByProject.merge(payable.getProject().getProjectId(),
                    accessContext.value(payable.getAmount()), BigDecimal::add);
        }
        BigDecimal estimatedCost = BigDecimal.ZERO;
        BigDecimal aggregatedTotalCost = BigDecimal.ZERO;
        BigDecimal aggregatedManualIncome = BigDecimal.ZERO;
        BigDecimal aggregatedManualCost = BigDecimal.ZERO;
        int incompleteProjects = 0;
        int incompleteItems = 0;
        Map<Long, ProjectCostContext.CostSummary> costSummaries = projectCostContext.summarizeAll(projects);
        Map<Long, ManualLedgerContext.ManualAmounts> manualAmounts = manualLedgerContext.amountsAll(projects);
        for (Project project : projects) {
            ProjectCostContext.CostSummary costSummary = costSummaries.get(project.getProjectId());
            if (costSummary == null) {
                costSummary = projectCostContext.summarize(project);
            }
            BigDecimal payableCost = payableCostByProject.getOrDefault(project.getProjectId(), BigDecimal.ZERO);
            ManualLedgerContext.ManualAmounts manual = manualAmounts.getOrDefault(
                    project.getProjectId(), ManualLedgerContext.ManualAmounts.ZERO);
            estimatedCost = estimatedCost.add(costSummary.estimatedCost());
            aggregatedTotalCost = aggregatedTotalCost.add(totalCost(project, costSummary, payableCost))
                    .add(manual.cost());
            aggregatedManualIncome = aggregatedManualIncome.add(manual.income());
            aggregatedManualCost = aggregatedManualCost.add(manual.cost());
            if (!costSummary.complete()) {
                incompleteProjects++;
            }
            incompleteItems += costSummary.incompleteItemCount();
        }
        boolean costComplete = incompleteProjects == 0;
        BigDecimal aggregatedRevenue = summary.getContractAmount().add(aggregatedManualIncome);
        summary.setEstimatedCost(estimatedCost);
        summary.setActualCost(aggregatedTotalCost);
        summary.setManualIncomeAmount(aggregatedManualIncome);
        summary.setManualCostAmount(aggregatedManualCost);
        summary.setCostComplete(costComplete);
        summary.setCostIncompleteProjectCount(incompleteProjects);
        summary.setCostIncompleteItemCount(incompleteItems);
        summary.setProfitAmount(costComplete
                ? aggregatedRevenue.subtract(aggregatedTotalCost) : null);
        // 与项目明细/结算同口径：成本不完整时毛利率同样为 null。
        summary.setGrossMargin(costComplete
                ? rate(summary.getProfitAmount(), aggregatedRevenue) : null);
        summary.setCollectionRate(rate(summary.getReceivedAmount(), summary.getReceivableAmount()));

        LocalDate today = LocalDate.now();
        List<Receivable> collectible = receivables.stream()
                .filter(item -> accessContext.outstanding(item.getAmount(), item.getReceivedAmount()).signum() > 0)
                .toList();
        summary.setUrgentReceivables(collectible.stream()
                .sorted(Comparator.comparing(Receivable::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8).toList());
        summary.setOverdueReceivables(collectible.stream()
                .filter(this::receivableOverdue)
                .sorted(Comparator.comparing(Receivable::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8).toList());
        summary.setDueSoonReceivables(collectible.stream()
                .filter(item -> !receivableOverdue(item))
                .filter(item -> item.getDueDate() != null
                        && !item.getDueDate().isBefore(today)
                        && !item.getDueDate().isAfter(today.plusDays(7)))
                .sorted(Comparator.comparing(Receivable::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8).toList());
        summary.setUrgentPayables(payables.stream()
                .filter(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()).signum() > 0)
                .sorted(Comparator.comparing(Payable::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8).toList());
        summary.setRecentLedgers(ledgers.stream()
                .sorted(Comparator.comparing(Ledger::getOccurredAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Ledger::getLedgerId,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .limit(8).toList());
        return summary;
    }

    public List<FinanceProjectReport> projectReports(Employee principal) {
        return projectReports(principal, null);
    }

    /** 项目账本（DEC-065 门店维度）：企业范围可下钻单店，门店范围恒为本店。 */
    public List<FinanceProjectReport> projectReports(Employee principal, Long storeId) {
        Employee current = accessContext.requireReader(principal);
        Long storeFilter = accessContext.resolveStoreFilter(current, storeId);
        return projects(current, storeFilter).stream()
                .map(project -> project(current, project.getProjectId()))
                .toList();
    }

    public FinanceProjectReport project(Employee principal, Long projectId) {
        Employee current = accessContext.requireReader(principal);
        Project project = accessContext.resolveProject(current, projectId);
        List<Receivable> receivables = receivableRepository.findByProjectOrderByDueDateAsc(project);
        List<Payable> payables = payableRepository.findByProjectOrderByDueDateAsc(project);
        List<Payable> costPayables = payables.stream()
                .filter(item -> BusinessOriginFilter.isNormal(item.getBusinessOrigin()))
                .toList();
        List<Ledger> ledgers = ledgerRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(
                        current.getCompany().getComId(), projectId)
                .stream()
                .filter(item -> !Boolean.TRUE.equals(item.getVoided()))
                .toList();
        List<Receipt> receipts = receiptRepository == null ? List.of()
                : receiptRepository.findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(
                        current.getCompany().getComId(), projectId);
        List<Payment> payments = paymentRepository == null ? List.of()
                : paymentRepository.findByCompany_ComIdAndProject_ProjectIdOrderByPaidAtDesc(
                        current.getCompany().getComId(), projectId);

        FinanceProjectReport report = new FinanceProjectReport();
        report.setProject(project);
        report.setContractAmount(accessContext.value(project.getContractAmount()));
        report.setReceivableAmount(sum(receivables.stream().map(Receivable::getAmount)));
        report.setReceivedAmount(sum(receivables.stream().map(Receivable::getReceivedAmount)));
        report.setOutstandingAmount(receivables.stream()
                .map(item -> accessContext.outstanding(item.getAmount(), item.getReceivedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setOverdueAmount(receivables.stream()
                .filter(this::receivableOverdue)
                .map(item -> accessContext.outstanding(item.getAmount(), item.getReceivedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setPayableAmount(sum(payables.stream().map(Payable::getAmount)));
        report.setPaidAmount(sum(payables.stream().map(Payable::getPaidAmount)));
        report.setUnpaidAmount(payables.stream()
                .map(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        YearMonth month = YearMonth.now();
        report.setReceiptCount(receipts.size());
        report.setPaymentCount(payments.size());
        report.setReceiptAmount(sum(receipts.stream().map(Receipt::getAmount)));
        report.setPaymentAmount(sum(payments.stream().map(Payment::getAmount)));
        report.setMonthReceiptAmount(sum(receipts.stream()
                .filter(item -> sameMonth(item.getReceivedAt(), month))
                .map(Receipt::getAmount)));
        report.setMonthPaymentAmount(sum(payments.stream()
                .filter(item -> sameMonth(item.getPaidAt(), month))
                .map(Payment::getAmount)));
        report.setUrgentPayableAmount(payables.stream()
                .filter(item -> item.getStatus() == PayableStatus.Overdue || Boolean.TRUE.equals(item.getUrgent()))
                .map(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal materialCost = payableAmount(costPayables, PayableType.Material);
        BigDecimal laborCost = payableAmount(costPayables, PayableType.Salary);
        BigDecimal projectCost = payableAmount(costPayables, PayableType.Project);
        BigDecimal expenseAllocation = sum(costPayables.stream()
                .filter(item -> item.getType() != PayableType.Material
                        && item.getType() != PayableType.Salary
                        && item.getType() != PayableType.Project)
                .map(Payable::getAmount));
        BigDecimal payableCost = materialCost.add(laborCost).add(projectCost).add(expenseAllocation);
        ProjectCostContext.CostSummary costSummary = projectCostContext.summarize(project);
        ManualLedgerContext.ManualAmounts manual = manualLedgerContext.amounts(project);
        BigDecimal totalCost = totalCost(project, costSummary, payableCost).add(manual.cost());
        boolean costComplete = costSummary.complete();
        BigDecimal projectRevenue = report.getContractAmount().add(manual.income());
        BigDecimal grossProfit = costComplete
                ? projectRevenue.subtract(totalCost) : null;
        report.setContractRevenue(report.getContractAmount());
        report.setMaterialAmount(materialCost);
        report.setMaterialCost(materialCost);
        report.setSalaryAmount(laborCost);
        report.setLaborCost(laborCost);
        report.setProjectPaymentAmount(projectCost);
        report.setProjectCost(projectCost);
        report.setOtherPayableAmount(expenseAllocation);
        report.setExpenseAllocation(expenseAllocation);
        report.setEstimatedCost(costSummary.estimatedCost());
        report.setActualCost(costSummary.actualCost().add(manual.cost()));
        report.setManualIncomeAmount(manual.income());
        report.setManualCostAmount(manual.cost());
        report.setCostComplete(costComplete);
        report.setCostIncompleteCount(costSummary.incompleteItemCount());
        report.setTotalCost(totalCost);
        report.setProfitAmount(grossProfit);
        report.setGrossProfit(grossProfit);
        report.setGrossMargin(grossProfit == null ? null : rate(grossProfit, projectRevenue));
        report.setCollectionRate(rate(report.getReceivedAmount(), report.getReceivableAmount()));
        report.setPaymentRate(rate(report.getPaidAmount(), report.getPayableAmount()));
        report.setReceivableCleared(!receivables.isEmpty() && receivables.stream()
                .allMatch(item -> accessContext.outstanding(item.getAmount(), item.getReceivedAmount()).signum() == 0));
        report.setPayableCleared(payables.stream()
                .allMatch(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()).signum() == 0));
        Settlement settlement = settlementRepository.findByProject_ProjectIdAndCompany_ComId(
                projectId, current.getCompany().getComId()).orElse(null);
        boolean historicalArchive = ProjectArchivePolicy.isHistoricalArchive(project);
        report.setHistoricalArchive(historicalArchive);
        report.setArchiveLabel(historicalArchive ? ProjectArchivePolicy.ARCHIVE_LABEL : null);
        report.setSettlementStatusLabel(historicalArchive && settlement == null
                ? ProjectArchivePolicy.SETTLEMENT_LABEL : null);
        report.setSettlementActionable(!historicalArchive);
        report.setSettlement(settlement);
        report.setReceivables(receivables);
        report.setPayables(payables);
        report.setRecentLedgers(ledgers.stream().limit(12).toList());
        return report;
    }

    public Settlement snapshot(Employee principal, Project project, Settlement settlement, Employee operator) {
        Employee current = accessContext.requireReader(principal);
        List<Receivable> receivables = receivableRepository.findByProjectOrderByDueDateAsc(project);
        List<Payable> payables = payableRepository.findByProjectOrderByDueDateAsc(project);
        List<Payable> costPayables = payables.stream()
                .filter(item -> BusinessOriginFilter.isNormal(item.getBusinessOrigin()))
                .toList();
        settlement.setCompany(current.getCompany());
        accessContext.applyOwnership(settlement, current, project);
        settlement.setProject(project);
        settlement.setContractAmount(accessContext.value(project.getContractAmount()));
        settlement.setReceivableAmount(sum(receivables.stream().map(Receivable::getAmount)));
        settlement.setReceivedAmount(sum(receivables.stream().map(Receivable::getReceivedAmount)));
        settlement.setPayableAmount(sum(payables.stream().map(Payable::getAmount)));
        settlement.setPaidAmount(sum(payables.stream().map(Payable::getPaidAmount)));
        BigDecimal materialCost = payableAmount(costPayables, PayableType.Material);
        BigDecimal laborCost = payableAmount(costPayables, PayableType.Salary);
        BigDecimal projectCost = payableAmount(costPayables, PayableType.Project);
        BigDecimal expenseAmount = sum(costPayables.stream()
                .filter(item -> item.getType() != PayableType.Material
                        && item.getType() != PayableType.Salary
                        && item.getType() != PayableType.Project)
                .map(Payable::getAmount));
        BigDecimal payableCost = materialCost.add(laborCost).add(projectCost).add(expenseAmount);
        ProjectCostContext.CostSummary costSummary = projectCostContext.summarize(project);
        ManualLedgerContext.ManualAmounts manual = manualLedgerContext.amounts(project);
        BigDecimal totalCost = totalCost(project, costSummary, payableCost).add(manual.cost());
        BigDecimal settlementRevenue = settlement.getContractAmount().add(manual.income());
        BigDecimal grossProfit = costSummary.complete()
                ? settlementRevenue.subtract(totalCost) : null;
        settlement.setEstimatedCost(costSummary.estimatedCost());
        settlement.setActualCost(costSummary.actualCost().add(manual.cost()));
        settlement.setManualIncomeAmount(manual.income());
        settlement.setManualCostAmount(manual.cost());
        settlement.setCostComplete(costSummary.complete());
        settlement.setCostIncompleteCount(costSummary.incompleteItemCount());
        settlement.setMaterialAmount(materialCost);
        settlement.setSalaryAmount(laborCost);
        settlement.setProjectAmount(projectCost);
        settlement.setExpenseAmount(expenseAmount);
        settlement.setTotalCost(totalCost);
        settlement.setProfitAmount(grossProfit);
        settlement.setGrossMargin(grossProfit == null ? null : rate(grossProfit, settlementRevenue));
        if (settlement.getCreatedBy() == null) {
            settlement.setCreatedBy(operator);
        }
        return settlement;
    }

    public boolean receivableCleared(Long projectId, Long comId) {
        List<Receivable> receivables = receivableRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByDueDateAsc(comId, projectId);
        return !receivables.isEmpty() && receivables.stream()
                .allMatch(item -> accessContext.outstanding(item.getAmount(), item.getReceivedAmount()).signum() == 0);
    }

    public boolean payableCleared(Long projectId, Long comId) {
        List<Payable> payables = payableRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByDueDateAsc(comId, projectId);
        return payables.stream()
                .allMatch(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()).signum() == 0);
    }

    /**
     * 按门店维度取项目（DEC-065）。
     *
     * <p>{@code storeFilter} 由 {@code resolveStoreFilter} 裁决：为 {@code null} 表示企业范围（全部门店），
     * 非空表示指定门店——门店范围主体的取值恒为其本店，不可能为空。因此无需再按角色硬编码，
     * 也不会出现「财务=全企业」与门店口径分叉。</p>
     */
    private List<Project> projects(Employee current, Long storeFilter) {
        Long comId = current.getCompany().getComId();
        if (storeFilter == null || storeFilter == StoreScope.ALL_STORES) {
            return projectRepository.findByCompany_ComIdOrderByProjectIdDesc(comId);
        }
        return projectRepository.findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(comId, storeFilter);
    }

    /**
     * 项目实际成本唯一口径：实际发生成本与应付成本取较高者；汇总、项目报告、结算共用。
     */
    private BigDecimal totalCost(Project project, ProjectCostContext.CostSummary costSummary, BigDecimal payableCost) {
        BigDecimal postActual = costSummary.actualCost();
        BigDecimal carryoverCost = BigDecimal.ZERO;
        if (carryoverCostContext != null) {
            carryoverCost = carryoverCostContext.totalCost(project);
            postActual = postActual.subtract(carryoverCost).max(BigDecimal.ZERO);
        }
        return carryoverCost.add(postActual.max(accessContext.value(payableCost)));
    }

    private BigDecimal payableAmount(List<Payable> payables, PayableType type) {
        return sum(payables.stream()
                .filter(item -> item.getType() == type)
                .map(Payable::getAmount));
    }

    private boolean belongsToArchivedProject(Project project, Set<Long> historicalArchiveProjectIds) {
        return project != null
                && project.getProjectId() != null
                && historicalArchiveProjectIds.contains(project.getProjectId());
    }

    private boolean receivableOverdue(Receivable receivable) {
        if (receivable.getStatus() == ReceivableStatus.Received
                || receivable.getStatus() == ReceivableStatus.Closed) {
            return false;
        }
        return receivable.getStatus() == ReceivableStatus.Overdue
                || (receivable.getDueDate() != null && receivable.getDueDate().isBefore(LocalDate.now()));
    }

    private boolean sameMonth(LocalDate date, YearMonth month) {
        return date != null && YearMonth.from(date).equals(month);
    }

    private BigDecimal sum(java.util.stream.Stream<BigDecimal> values) {
        return values.filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal base = accessContext.value(denominator);
        if (base.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return accessContext.value(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(base, 1, RoundingMode.HALF_UP);
    }
}
