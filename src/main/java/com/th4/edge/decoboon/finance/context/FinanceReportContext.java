package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 财务汇总、项目财务全景与结算快照计算。 */
@Component
public class FinanceReportContext {

    private final FinanceAccessContext accessContext;
    private final ProjectRepository projectRepository;
    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;
    private final LedgerRepository ledgerRepository;
    private final SettlementRepository settlementRepository;

    public FinanceReportContext(FinanceAccessContext accessContext,
                                ProjectRepository projectRepository,
                                ReceivableRepository receivableRepository,
                                PayableRepository payableRepository,
                                LedgerRepository ledgerRepository,
                                SettlementRepository settlementRepository) {
        this.accessContext = accessContext;
        this.projectRepository = projectRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
        this.ledgerRepository = ledgerRepository;
        this.settlementRepository = settlementRepository;
    }

    public FinanceSummary summary(Employee principal) {
        Employee current = accessContext.requireReader(principal);
        List<Project> projects = projects(current);
        List<Receivable> receivables = receivableRepository.findAll(accessContext.visible(current, Receivable.class));
        List<Payable> payables = payableRepository.findAll(accessContext.visible(current, Payable.class));
        List<Ledger> ledgers = ledgerRepository.findAll(accessContext.visible(current, Ledger.class));

        FinanceSummary summary = new FinanceSummary();
        summary.setProjectCount(projects.size());
        summary.setActiveProjectCount((int) projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.Preparing
                        || project.getStatus() == ProjectStatus.Ongoing
                        || project.getStatus() == ProjectStatus.Accepting)
                .count());
        summary.setContractAmount(sum(projects.stream().map(Project::getContractAmount)));
        summary.setReceivableAmount(sum(receivables.stream().map(Receivable::getAmount)));
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
        summary.setCashBalance(ledgers.stream()
                .map(item -> item.getDirection() == LedgerDirection.Income
                        ? accessContext.value(item.getAmount()) : accessContext.value(item.getAmount()).negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setProfitAmount(summary.getContractAmount().subtract(summary.getPayableAmount()));
        summary.setCollectionRate(rate(summary.getReceivedAmount(), summary.getReceivableAmount()));

        summary.setUrgentReceivables(receivables.stream()
                .filter(item -> accessContext.outstanding(item.getAmount(), item.getReceivedAmount()).signum() > 0)
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
        Employee current = accessContext.requireReader(principal);
        return projects(current).stream()
                .map(project -> project(current, project.getProjectId()))
                .toList();
    }

    public FinanceProjectReport project(Employee principal, Long projectId) {
        Employee current = accessContext.requireReader(principal);
        Project project = accessContext.resolveProject(current, projectId);
        List<Receivable> receivables = receivableRepository.findByProjectOrderByDueDateAsc(project);
        List<Payable> payables = payableRepository.findByProjectOrderByDueDateAsc(project);
        List<Ledger> ledgers = ledgerRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(
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
        BigDecimal materialCost = payableAmount(payables, PayableType.Material);
        BigDecimal laborCost = payableAmount(payables, PayableType.Salary);
        BigDecimal projectCost = payableAmount(payables, PayableType.Project);
        BigDecimal expenseAllocation = sum(payables.stream()
                .filter(item -> item.getType() != PayableType.Material
                        && item.getType() != PayableType.Salary
                        && item.getType() != PayableType.Project)
                .map(Payable::getAmount));
        BigDecimal payableCost = materialCost.add(laborCost).add(projectCost).add(expenseAllocation);
        // 项目立项时会冻结报价成本；尚未拆分应付单时，预计利润仍必须扣减该基线成本。
        BigDecimal totalCost = payableCost.signum() > 0
                ? payableCost : accessContext.value(project.getCost());
        BigDecimal grossProfit = report.getContractAmount().subtract(totalCost);
        report.setContractRevenue(report.getContractAmount());
        report.setMaterialAmount(materialCost);
        report.setMaterialCost(materialCost);
        report.setSalaryAmount(laborCost);
        report.setLaborCost(laborCost);
        report.setProjectPaymentAmount(projectCost);
        report.setProjectCost(projectCost);
        report.setOtherPayableAmount(expenseAllocation);
        report.setExpenseAllocation(expenseAllocation);
        report.setTotalCost(totalCost);
        report.setProfitAmount(grossProfit);
        report.setGrossProfit(grossProfit);
        report.setGrossMargin(rate(grossProfit, report.getContractAmount()));
        report.setCollectionRate(rate(report.getReceivedAmount(), report.getReceivableAmount()));
        report.setPaymentRate(rate(report.getPaidAmount(), report.getPayableAmount()));
        report.setReceivableCleared(!receivables.isEmpty() && receivables.stream()
                .allMatch(item -> accessContext.outstanding(item.getAmount(), item.getReceivedAmount()).signum() == 0));
        report.setPayableCleared(payables.stream()
                .allMatch(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()).signum() == 0));
        report.setSettlement(settlementRepository.findByProject_ProjectIdAndCompany_ComId(
                projectId, current.getCompany().getComId()).orElse(null));
        report.setReceivables(receivables);
        report.setPayables(payables);
        report.setRecentLedgers(ledgers.stream().limit(12).toList());
        return report;
    }

    public Settlement snapshot(Employee principal, Project project, Settlement settlement, Employee operator) {
        Employee current = accessContext.requireReader(principal);
        List<Receivable> receivables = receivableRepository.findByProjectOrderByDueDateAsc(project);
        List<Payable> payables = payableRepository.findByProjectOrderByDueDateAsc(project);
        settlement.setCompany(current.getCompany());
        accessContext.applyOwnership(settlement, current, project);
        settlement.setProject(project);
        settlement.setContractAmount(accessContext.value(project.getContractAmount()));
        settlement.setReceivableAmount(sum(receivables.stream().map(Receivable::getAmount)));
        settlement.setReceivedAmount(sum(receivables.stream().map(Receivable::getReceivedAmount)));
        settlement.setPayableAmount(sum(payables.stream().map(Payable::getAmount)));
        settlement.setPaidAmount(sum(payables.stream().map(Payable::getPaidAmount)));
        BigDecimal materialCost = payableAmount(payables, PayableType.Material);
        BigDecimal laborCost = payableAmount(payables, PayableType.Salary);
        BigDecimal projectCost = payableAmount(payables, PayableType.Project);
        BigDecimal expenseAmount = sum(payables.stream()
                .filter(item -> item.getType() != PayableType.Material
                        && item.getType() != PayableType.Salary
                        && item.getType() != PayableType.Project)
                .map(Payable::getAmount));
        BigDecimal totalCost = materialCost.add(laborCost).add(projectCost).add(expenseAmount);
        BigDecimal grossProfit = settlement.getContractAmount().subtract(totalCost);
        settlement.setMaterialAmount(materialCost);
        settlement.setSalaryAmount(laborCost);
        settlement.setProjectAmount(projectCost);
        settlement.setExpenseAmount(expenseAmount);
        settlement.setTotalCost(totalCost);
        settlement.setProfitAmount(grossProfit);
        settlement.setGrossMargin(rate(grossProfit, settlement.getContractAmount()));
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

    private List<Project> projects(Employee current) {
        if (current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.Finance) {
            return projectRepository.findByCompany_ComIdOrderByProjectIdDesc(current.getCompany().getComId());
        }
        if (current.getRole() == EmployeeRole.StoreManager
                && current.getStore() != null && current.getStore().getStoreId() != null) {
            return projectRepository.findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(
                    current.getCompany().getComId(), current.getStore().getStoreId());
        }
        return List.of();
    }

    private BigDecimal payableAmount(List<Payable> payables, PayableType type) {
        return sum(payables.stream()
                .filter(item -> item.getType() == type)
                .map(Payable::getAmount));
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
