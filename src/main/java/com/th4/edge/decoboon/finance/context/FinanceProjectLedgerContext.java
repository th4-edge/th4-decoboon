package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.decoboon.carryover.repository.CarryoverCostSnapshotRepository;
import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.common.context.BusinessOriginFilter;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerManualPurpose;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.configure.ReceiptStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.SettlementStatus;
import com.th4.edge.decoboon.finance.model.FinanceProjectLedgerReport;
import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 项目账本批量读取与筛选，避免逐项目查询形成 N+1。 */
@Component
public class FinanceProjectLedgerContext {

    private final FinanceAccessContext accessContext;
    private final ProjectRepository projectRepository;
    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;
    private final LedgerRepository ledgerRepository;
    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final CarryoverCostSnapshotRepository carryoverCostRepository;
    private final ProjectCostContext projectCostContext;

    public FinanceProjectLedgerContext(FinanceAccessContext accessContext,
                                       ProjectRepository projectRepository,
                                       ReceivableRepository receivableRepository,
                                       PayableRepository payableRepository,
                                       LedgerRepository ledgerRepository,
                                       ReceiptRepository receiptRepository,
                                       PaymentRepository paymentRepository,
                                       SettlementRepository settlementRepository,
                                       CarryoverCostSnapshotRepository carryoverCostRepository,
                                       ProjectCostContext projectCostContext) {
        this.accessContext = accessContext;
        this.projectRepository = projectRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
        this.ledgerRepository = ledgerRepository;
        this.receiptRepository = receiptRepository;
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.carryoverCostRepository = carryoverCostRepository;
        this.projectCostContext = projectCostContext;
    }

    public Page<FinanceProjectReport> page(Employee principal, Long storeId, String keyword, String status,
                                           String scope, String filter, int current, int size) {
        List<FinanceProjectReport> reports = filteredReports(principal, storeId, keyword, status, scope, filter);
        int page = Math.max(current, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 100);
        int from = Math.min(page * pageSize, reports.size());
        int to = Math.min(from + pageSize, reports.size());
        return new PageImpl<>(reports.subList(from, to), PageRequest.of(page, pageSize), reports.size());
    }

    public FinanceProjectLedgerReport summary(Employee principal, Long storeId, String keyword, String status,
                                              String scope, String filter) {
        List<FinanceProjectReport> reports = filteredReports(principal, storeId, keyword, status, scope, filter);
        BigDecimal contract = sum(reports.stream().map(FinanceProjectReport::getContractAmount));
        BigDecimal receivable = sum(reports.stream().map(FinanceProjectReport::getReceivableAmount));
        BigDecimal received = sum(reports.stream().map(FinanceProjectReport::getReceivedAmount));
        int active = (int) reports.stream().filter(report -> report.getProject() != null
                && Set.of(ProjectStatus.Preparing, ProjectStatus.Ongoing, ProjectStatus.Accepting)
                .contains(report.getProject().getStatus())).count();
        int incomplete = (int) reports.stream().filter(report -> !Boolean.TRUE.equals(report.getCostComplete())).count();

        FinanceProjectLedgerReport summary = new FinanceProjectLedgerReport();
        summary.setProjectCount(reports.size());
        summary.setActiveProjectCount(active);
        summary.setContractAmount(money(contract));
        summary.setReceivableAmount(money(receivable));
        summary.setReceivedAmount(money(received));
        summary.setCollectionRate(rate(received, receivable));
        summary.setCostComplete(incomplete == 0);
        summary.setCostIncompleteProjectCount(incomplete);
        summary.setAsOf(LocalDateTime.now());
        return summary;
    }

    private List<FinanceProjectReport> filteredReports(Employee principal, Long storeId, String keyword,
                                                       String status, String scope, String filter) {
        Employee current = accessContext.requireReader(principal);
        Long storeFilter = accessContext.resolveStoreFilter(current, storeId);
        List<Project> projects = loadProjects(current, storeFilter, keyword, status, scope);
        if (projects.isEmpty()) {
            return List.of();
        }

        List<Receivable> receivables = receivableRepository.findByProject_ProjectIdInOrderByDueDateAsc(
                projects.stream().map(Project::getProjectId).toList());
        List<Payable> payables = payableRepository.findByCompany_ComIdAndProject_ProjectIdIn(
                current.getCompany().getComId(), projects.stream().map(Project::getProjectId).toList());
        List<Ledger> ledgers = ledgerRepository.findByCompany_ComIdAndProject_ProjectIdIn(
                        current.getCompany().getComId(), projects.stream().map(Project::getProjectId).toList())
                .stream().filter(item -> !Boolean.TRUE.equals(item.getVoided())).toList();
        List<Receipt> receipts = receiptRepository.findByCompany_ComIdAndProject_ProjectIdIn(
                current.getCompany().getComId(), projects.stream().map(Project::getProjectId).toList());
        List<Payment> payments = paymentRepository.findByCompany_ComIdAndProject_ProjectIdIn(
                current.getCompany().getComId(), projects.stream().map(Project::getProjectId).toList());
        List<Settlement> settlements = settlementRepository.findByCompany_ComIdAndProject_ProjectIdIn(
                current.getCompany().getComId(), projects.stream().map(Project::getProjectId).toList());

        Map<Long, List<Receivable>> receivablesByProject = groupReceivables(receivables);
        Map<Long, List<Payable>> payablesByProject = groupPayables(payables);
        Map<Long, List<Ledger>> ledgersByProject = groupLedgers(ledgers);
        Map<Long, List<Receipt>> receiptsByProject = groupReceipts(receipts);
        Map<Long, List<Payment>> paymentsByProject = groupPayments(payments);
        Map<Long, Settlement> settlementsByProject = settlements.stream()
                .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                .collect(Collectors.toMap(item -> item.getProject().getProjectId(), item -> item, (left, right) -> left));
        Map<Long, ProjectCostContext.CostSummary> costs = projectCostContext.summarizeAll(projects);
        Map<Long, ManualAmounts> manuals = manualAmounts(ledgersByProject);

        List<FinanceProjectReport> reports = new ArrayList<>(projects.size());
        for (Project project : projects) {
            List<Receivable> projectReceivables = receivablesByProject.getOrDefault(project.getProjectId(), List.of());
            List<Payable> projectPayables = payablesByProject.getOrDefault(project.getProjectId(), List.of());
            List<Ledger> projectLedgers = ledgersByProject.getOrDefault(project.getProjectId(), List.of());
            List<Receipt> projectReceipts = receiptsByProject.getOrDefault(project.getProjectId(), List.of());
            List<Payment> projectPayments = paymentsByProject.getOrDefault(project.getProjectId(), List.of());
            reports.add(report(project, projectReceivables, projectPayables, projectLedgers, projectReceipts,
                    projectPayments, settlementsByProject.get(project.getProjectId()),
                    costs.get(project.getProjectId()),
                    manuals.getOrDefault(project.getProjectId(), ManualAmounts.ZERO)));
        }
        return reports.stream()
                .filter(report -> matchesFilter(report, filter))
                .sorted(Comparator.comparing((FinanceProjectReport report) -> report.getProject().getProjectId(),
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    private List<Project> loadProjects(Employee current, Long storeFilter, String keyword, String status, String scope) {
        Specification<Project> specification = accessContext.visible(current, Project.class);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        specification = specification.and((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StoreScope.isAllStores(storeFilter)) {
                Expression<Long> projectStore = cb.coalesce(
                        root.get("store").get("storeId"),
                        root.get("customer").get("store").get("storeId"));
                predicates.add(cb.equal(projectStore, storeFilter));
            }
            if (status != null && !status.isBlank()) {
                ProjectStatus parsed = enumValue(ProjectStatus.class, status);
                predicates.add(parsed == null ? cb.disjunction() : cb.equal(root.get("status"), parsed));
            }
            if (!kw.isBlank()) {
                String pattern = "%" + kw + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("name"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("no"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("siteAddr"), "")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        });

        List<Project> base = projectRepository.findLedgerRows(specification).stream()
                .filter(project -> matchesScope(project, scope))
                .sorted(Comparator.comparing(Project::getProjectId,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        if (base.isEmpty()) {
            return List.of();
        }
        Map<Long, Project> details = projectRepository.findDetailsByIdIn(
                        base.stream().map(Project::getProjectId).toList()).stream()
                .collect(Collectors.toMap(Project::getProjectId, item -> item, (left, right) -> left));
        return base.stream().map(item -> details.getOrDefault(item.getProjectId(), item)).toList();
    }

    private FinanceProjectReport report(Project project, List<Receivable> receivables, List<Payable> payables,
                                        List<Ledger> ledgers, List<Receipt> receipts, List<Payment> payments,
                                        Settlement settlement, ProjectCostContext.CostSummary costSummary,
                                        ManualAmounts manual) {
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
        List<Receipt> activeReceipts = receipts.stream()
                .filter(item -> item.getStatus() == ReceiptStatus.Active)
                .toList();
        BigDecimal grossReceiptAmount = sum(receipts.stream().map(Receipt::getAmount));
        BigDecimal activeReceiptAmount = sum(activeReceipts.stream().map(Receipt::getAmount));
        report.setGrossReceiptAmount(grossReceiptAmount);
        report.setReversalAmount(grossReceiptAmount.subtract(activeReceiptAmount));
        report.setReceiptCount(activeReceipts.size());
        report.setPaymentCount(payments.size());
        report.setReceiptAmount(activeReceiptAmount);
        report.setPaymentAmount(sum(payments.stream().map(Payment::getAmount)));
        report.setMonthReceiptAmount(sum(activeReceipts.stream()
                .filter(item -> sameMonth(item.getReceivedAt(), month))
                .map(Receipt::getAmount)));
        report.setMonthPaymentAmount(sum(payments.stream()
                .filter(item -> sameMonth(item.getPaidAt(), month))
                .map(Payment::getAmount)));
        report.setUrgentPayableAmount(payables.stream()
                .filter(item -> item.getStatus() == PayableStatus.Overdue || Boolean.TRUE.equals(item.getUrgent()))
                .map(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        ProjectCostContext.CostSummary safeCost = costSummary == null
                ? new ProjectCostContext.CostSummary(BigDecimal.ZERO, BigDecimal.ZERO, 1) : costSummary;
        BigDecimal materialCost = safeCost.materialCost();
        BigDecimal laborCost = safeCost.workerLaborCost();
        BigDecimal projectCost = safeCost.projectExpenseCost().add(safeCost.otherProjectAccruedCost());
        BigDecimal expenseAllocation = safeCost.employeeSalaryCost().add(safeCost.employeeCommissionCost());
        BigDecimal totalCost = safeCost.actualCost();
        boolean costComplete = safeCost.complete();
        BigDecimal projectRevenue = report.getContractAmount().add(manual.income());
        BigDecimal grossProfit = costComplete ? projectRevenue.subtract(totalCost) : null;

        report.setContractRevenue(report.getContractAmount());
        report.setMaterialAmount(materialCost);
        report.setMaterialCost(materialCost);
        report.setSalaryAmount(laborCost);
        report.setLaborCost(laborCost);
        report.setProjectPaymentAmount(projectCost);
        report.setProjectCost(projectCost);
        report.setOtherPayableAmount(expenseAllocation);
        report.setExpenseAllocation(expenseAllocation);
        report.setEstimatedCost(safeCost.estimatedCost());
        report.setActualCost(safeCost.actualCost());
        report.setManualIncomeAmount(manual.income());
        report.setManualCostAmount(manual.cost());
        report.setCostComplete(costComplete);
        report.setCostIncompleteCount(safeCost.incompleteItemCount());
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
        boolean historicalArchive = ProjectArchivePolicy.isHistoricalArchive(project);
        report.setHistoricalArchive(historicalArchive);
        report.setArchiveLabel(historicalArchive ? ProjectArchivePolicy.ARCHIVE_LABEL : null);
        report.setSettlementStatusLabel(historicalArchive && settlement == null
                ? ProjectArchivePolicy.SETTLEMENT_LABEL : null);
        report.setSettlementActionable(!historicalArchive);
        report.setSettlement(settlement);
        report.setRecentLedgers(ledgers.stream().limit(12).toList());
        return report;
    }

    private boolean matchesScope(Project project, String value) {
        boolean historicalArchive = ProjectArchivePolicy.isHistoricalArchive(project);
        if (value == null || value.isBlank() || "current".equalsIgnoreCase(value.trim())) {
            return !historicalArchive;
        }
        if ("archive".equalsIgnoreCase(value.trim())) {
            return historicalArchive;
        }
        return true;
    }

    private boolean matchesFilter(FinanceProjectReport report, String filter) {
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            return true;
        }
        if ("risk".equalsIgnoreCase(filter)) {
            return report.getOverdueAmount().signum() > 0
                    || report.getUnpaidAmount().signum() > 0
                    || !Boolean.TRUE.equals(report.getCostComplete());
        }
        if ("settle".equalsIgnoreCase(filter)) {
            SettlementStatus status = report.getSettlement() == null ? null : report.getSettlement().getStatus();
            return status == SettlementStatus.Draft || status == SettlementStatus.Confirmed
                    || (report.getSettlement() == null && report.getProject().getStatus() == ProjectStatus.Finished);
        }
        return true;
    }

    private Map<Long, ManualAmounts> manualAmounts(Map<Long, List<Ledger>> ledgersByProject) {
        Map<Long, ManualAmounts> result = new LinkedHashMap<>();
        ledgersByProject.forEach((projectId, items) -> {
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal cost = BigDecimal.ZERO;
            for (Ledger ledger : items) {
                if (Boolean.TRUE.equals(ledger.getVoided())
                        || ledger.getSource() != LedgerSource.Manual
                        || ledger.getPurpose() == null) {
                    continue;
                }
                BigDecimal amount = accessContext.value(ledger.getAmount());
                if (ledger.getDirection() == LedgerDirection.Income
                        && ledger.getPurpose() == LedgerManualPurpose.OtherIncome) {
                    income = income.add(amount);
                } else if (ledger.getDirection() == LedgerDirection.Expense
                        && ledger.getPurpose() == LedgerManualPurpose.ProjectIncidentalCost) {
                    cost = cost.add(amount);
                }
            }
            result.put(projectId, new ManualAmounts(income, cost));
        });
        return result;
    }

    private Map<Long, List<Receivable>> groupReceivables(List<Receivable> items) {
        return items.stream().filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                .collect(Collectors.groupingBy(item -> item.getProject().getProjectId()));
    }

    private Map<Long, List<Payable>> groupPayables(List<Payable> items) {
        return items.stream().filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                .collect(Collectors.groupingBy(item -> item.getProject().getProjectId()));
    }

    private Map<Long, List<Ledger>> groupLedgers(List<Ledger> items) {
        return items.stream().filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                .collect(Collectors.groupingBy(item -> item.getProject().getProjectId()));
    }

    private Map<Long, List<Receipt>> groupReceipts(List<Receipt> items) {
        return items.stream().filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                .collect(Collectors.groupingBy(item -> item.getProject().getProjectId()));
    }

    private Map<Long, List<Payment>> groupPayments(List<Payment> items) {
        return items.stream().filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                .collect(Collectors.groupingBy(item -> item.getProject().getProjectId()));
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
        return values.filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal base = accessContext.value(denominator);
        if (base.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return accessContext.value(numerator).multiply(BigDecimal.valueOf(100))
                .divide(base, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private <E extends Enum<E>> E enumValue(Class<E> enumType, String rawValue) {
        for (E value : enumType.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(rawValue.trim())) {
                return value;
            }
        }
        return null;
    }

    private record ManualAmounts(BigDecimal income, BigDecimal cost) {
        private static final ManualAmounts ZERO = new ManualAmounts(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
