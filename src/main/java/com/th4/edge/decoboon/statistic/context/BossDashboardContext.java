package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BossDashboardQuery;
import com.th4.edge.decoboon.statistic.model.BusinessTarget;
import com.th4.edge.decoboon.statistic.model.DashboardMetric;
import com.th4.edge.decoboon.statistic.repository.BusinessTargetRepository;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 老板驾驶舱指标计算。 */
@Component
public class BossDashboardContext {

    private final TenantContext tenantContext;
    private final ProjectRepository projectRepository;
    private final ProjectContractRepository contractRepository;
    private final ReceiptRepository receiptRepository;
    private final PayableRepository payableRepository;
    private final BusinessTargetRepository targetRepository;

    public BossDashboardContext(TenantContext tenantContext,
                                ProjectRepository projectRepository,
                                ProjectContractRepository contractRepository,
                                ReceiptRepository receiptRepository,
                                PayableRepository payableRepository,
                                BusinessTargetRepository targetRepository) {
        this.tenantContext = tenantContext;
        this.projectRepository = projectRepository;
        this.contractRepository = contractRepository;
        this.receiptRepository = receiptRepository;
        this.payableRepository = payableRepository;
        this.targetRepository = targetRepository;
    }

    public BossDashboard dashboard(Employee principal, BossDashboardQuery query) {
        Employee current = tenantContext.requireBoss(principal);
        Long comId = current.getCompany().getComId();
        LocalDate month = query == null || query.getMonth() == null
                ? LocalDate.now().withDayOfMonth(1)
                : query.getMonth().withDayOfMonth(1);
        LocalDate previousMonth = month.minusMonths(1);
        LocalDate previousYear = month.minusYears(1);

        Long storeId = query == null ? null : query.getStoreId();
        List<Project> projects = projectRepository.findByCompany_ComIdOrderByProjectIdDesc(comId);
        List<ProjectContract> contracts = contractRepository.findByCompany_ComIdOrderByContractIdDesc(comId);
        List<Receipt> receipts = receiptRepository.findByCompany_ComIdOrderByReceivedAtDesc(comId);
        List<Payable> payables = payableRepository.findByCompany_ComIdOrderByDueDateAsc(comId);
        if (storeId != null) {
            // 门店维度：仅统计指定门店的合同、回款、应付与项目
            projects = projects.stream().filter(item -> inStore(item.getStore(), storeId)).toList();
            contracts = contracts.stream().filter(item -> inStore(item.getStore(), storeId)).toList();
            receipts = receipts.stream().filter(item -> inStore(item.getStore(), storeId)).toList();
            payables = payables.stream().filter(item -> inStore(item.getStore(), storeId)).toList();
        }
        BusinessTarget target = targetRepository.findByCompany_ComIdAndTargetMonth(comId, month)
                .orElseGet(BusinessTarget::new);

        BigDecimal signed = signedAmount(contracts, month);
        BigDecimal previousMonthSigned = signedAmount(contracts, previousMonth);
        BigDecimal previousYearSigned = signedAmount(contracts, previousYear);
        BigDecimal received = receivedAmount(receipts, month);
        BigDecimal previousMonthReceived = receivedAmount(receipts, previousMonth);
        BigDecimal previousYearReceived = receivedAmount(receipts, previousYear);
        BigDecimal profit = grossProfit(contracts, payables, month);
        BigDecimal previousMonthProfit = grossProfit(contracts, payables, previousMonth);
        BigDecimal previousYearProfit = grossProfit(contracts, payables, previousYear);
        BigDecimal active = activeProjectCount(projects, contracts, month);
        BigDecimal previousMonthActive = activeProjectCount(projects, contracts, previousMonth);
        BigDecimal previousYearActive = activeProjectCount(projects, contracts, previousYear);

        BossDashboard dashboard = new BossDashboard();
        dashboard.setMonth(month);
        dashboard.setSignedAmount(metric(signed, target.getSignedAmount(), previousMonthSigned, previousYearSigned));
        dashboard.setReceivedAmount(metric(received, target.getReceivedAmount(), previousMonthReceived, previousYearReceived));
        dashboard.setGrossProfit(metric(profit, target.getGrossProfit(), previousMonthProfit, previousYearProfit));
        dashboard.setActiveProjectCount(metric(active, BigDecimal.valueOf(value(target.getActiveProjectCount())),
                previousMonthActive, previousYearActive));
        return dashboard;
    }

    private DashboardMetric metric(BigDecimal value, BigDecimal target, BigDecimal previousMonth, BigDecimal previousYear) {
        DashboardMetric metric = new DashboardMetric();
        metric.setValue(value);
        metric.setTarget(value(target));
        metric.setAchievementRate(percentage(value, value(target)));
        metric.setPreviousMonthValue(value(previousMonth));
        metric.setPreviousYearValue(value(previousYear));
        metric.setMonthOverMonthRate(changeRate(value, value(previousMonth)));
        metric.setYearOverYearRate(changeRate(value, value(previousYear)));
        return metric;
    }

    private BigDecimal signedAmount(List<ProjectContract> contracts, LocalDate month) {
        return contracts.stream()
                .filter(contract -> sameMonth(contract.getSignedAt(), month))
                .map(this::contractRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal receivedAmount(List<Receipt> receipts, LocalDate month) {
        return receipts.stream()
                .filter(receipt -> sameMonth(receipt.getReceivedAt(), month))
                .map(Receipt::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal grossProfit(List<ProjectContract> contracts, List<Payable> payables, LocalDate month) {
        Map<Long, BigDecimal> costByProject = new HashMap<>();
        for (Payable payable : payables) {
            if (payable.getProject() == null || payable.getProject().getProjectId() == null) {
                continue;
            }
            costByProject.merge(payable.getProject().getProjectId(), value(payable.getAmount()), BigDecimal::add);
        }
        return contracts.stream()
                .filter(contract -> sameMonth(contract.getSignedAt(), month))
                .map(contract -> {
                    Project project = contract.getProject();
                    BigDecimal payableCost = project == null ? BigDecimal.ZERO
                            : costByProject.getOrDefault(project.getProjectId(), BigDecimal.ZERO);
                    BigDecimal cost = payableCost.signum() > 0 ? payableCost
                            : (project == null ? BigDecimal.ZERO : value(project.getCost()));
                    return contractRevenue(contract).subtract(cost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal activeProjectCount(List<Project> projects, List<ProjectContract> contracts, LocalDate month) {
        LocalDate end = YearMonth.from(month).atEndOfMonth();
        Map<Long, LocalDate> signedByProject = new HashMap<>();
        for (ProjectContract contract : contracts) {
            if (contract.getProject() != null && contract.getSignedAt() != null) {
                signedByProject.put(contract.getProject().getProjectId(), contract.getSignedAt());
            }
        }
        long count = projects.stream().filter(project -> {
            LocalDate started = signedByProject.get(project.getProjectId());
            if (started == null && project.getCreateTime() != null) {
                started = project.getCreateTime().toLocalDate();
            }
            LocalDate ended = project.getActualEndDate();
            return started != null && !started.isAfter(end) && (ended == null || ended.isAfter(end));
        }).count();
        return BigDecimal.valueOf(count);
    }

    private BigDecimal contractRevenue(ProjectContract contract) {
        return value(contract.getCurrentAmount()).signum() == 0
                ? value(contract.getBaselineAmount()) : value(contract.getCurrentAmount());
    }

    private boolean sameMonth(LocalDate date, LocalDate month) {
        return date != null && YearMonth.from(date).equals(YearMonth.from(month));
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return value(numerator).multiply(BigDecimal.valueOf(100))
                .divide(denominator, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal changeRate(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /** 门店范围判定：storeId 为空表示全部门店。 */
    private boolean inStore(com.th4.edge.tenant.user.model.Store store, Long storeId) {
        if (storeId == null) {
            return true;
        }
        return store != null && storeId.equals(store.getStoreId());
    }
}
