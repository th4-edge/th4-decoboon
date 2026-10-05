package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverDifferenceStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverDifferenceType;
import com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationTheme;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationItem;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationReport;
import com.th4.edge.decoboon.carryover.repository.CarryoverBatchRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverReconciliationItemRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverReconciliationRepository;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 首月双轨对账：四个主题原账/装得清对照与差异处理。 */
@Component
public class CarryoverReconciliationContext {

    private final CarryoverAccessContext accessContext;
    private final CarryoverBatchRepository batchRepository;
    private final CarryoverReconciliationRepository reconciliationRepository;
    private final CarryoverReconciliationItemRepository itemRepository;
    private final FinanceReportContext financeReportContext;

    public CarryoverReconciliationContext(CarryoverAccessContext accessContext,
                                          CarryoverBatchRepository batchRepository,
                                          CarryoverReconciliationRepository reconciliationRepository,
                                          CarryoverReconciliationItemRepository itemRepository,
                                          FinanceReportContext financeReportContext) {
        this.accessContext = accessContext;
        this.batchRepository = batchRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.itemRepository = itemRepository;
        this.financeReportContext = financeReportContext;
    }

    @Transactional
    public CarryoverReconciliationReport save(Employee principal, Long batchId,
                                              LocalDate periodStart, LocalDate periodEnd, String remark) {
        Employee current = accessContext.requireFinance(principal);
        var batch = batchRepository.findByBatchIdAndCompany_ComId(batchId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接任务不存在或无权访问")));
        CarryoverReconciliation reconciliation = reconciliationRepository
                .findFirstByCompany_ComIdAndBatch_BatchIdOrderByReconciliationIdDesc(
                        current.getCompany().getComId(), batchId)
                .orElseGet(CarryoverReconciliation::new);
        if (reconciliation.getReconciliationId() == null) {
            reconciliation.setCompany(current.getCompany());
            reconciliation.setBatch(batch);
            reconciliation.setCreatedBy(current);
            reconciliation.setOwner(current);
            reconciliation.setAsOfDate(batch.getAsOfDate() == null ? LocalDate.now() : batch.getAsOfDate());
            reconciliation.setStatus(CarryoverReconciliationStatus.Draft);
        }
        LocalDate start = periodStart == null ? reconciliation.getAsOfDate() : periodStart;
        reconciliation.setPeriodStart(start);
        reconciliation.setPeriodEnd(periodEnd == null ? start.with(TemporalAdjusters.lastDayOfMonth()) : periodEnd);
        if (remark != null) reconciliation.setRemark(remark.trim());
        reconciliation = reconciliationRepository.save(reconciliation);
        refreshSystemAmounts(current, reconciliation, true);
        return report(current, reconciliation);
    }

    @Transactional
    public CarryoverReconciliationReport report(Employee principal, Long reconciliationId) {
        Employee current = accessContext.requireFinance(principal);
        CarryoverReconciliation reconciliation = reconciliationRepository
                .findByCompany_ComIdAndReconciliationId(current.getCompany().getComId(), reconciliationId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "对账批次不存在或无权访问")));
        refreshSystemAmounts(current, reconciliation, false);
        return report(current, reconciliation);
    }

    @Transactional
    public CarryoverReconciliationReport saveItem(Employee principal, Long itemId,
                                                  BigDecimal originalAmount, String originalSource,
                                                  Boolean originalConfirmed,
                                                  CarryoverDifferenceType differenceType,
                                                  CarryoverDifferenceStatus differenceStatus,
                                                  String reason, String action, Long ownerId, LocalDate dueDate) {
        Employee current = accessContext.requireFinance(principal);
        CarryoverReconciliationItem item = itemRepository
                .findByCompany_ComIdAndItemId(current.getCompany().getComId(), itemId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "对账主题不存在或无权访问")));
        CarryoverReconciliation reconciliation = item.getReconciliation();
        refreshSystemAmounts(current, reconciliation, true);
        item = itemRepository.findByCompany_ComIdAndItemId(current.getCompany().getComId(), itemId).orElseThrow();
        if (originalAmount != null) item.setOriginalAmount(originalAmount);
        if (originalSource != null) item.setOriginalSource(originalSource.trim());
        if (originalConfirmed != null) item.setOriginalConfirmed(originalConfirmed);
        if (differenceType != null) item.setDifferenceType(differenceType);
        if (differenceStatus != null) item.setDifferenceStatus(differenceStatus);
        if (reason != null) item.setReason(reason.trim());
        if (action != null) item.setAction(action.trim());
        if (ownerId != null) {
            Employee owner = new Employee();
            owner.setEmpId(ownerId);
            item.setOwner(owner);
        }
        if (dueDate != null) item.setDueDate(dueDate);
        item.setDifference(difference(item.getOriginalAmount(), item.getSystemAmount()));
        itemRepository.save(item);
        updateStatus(reconciliation);
        reconciliationRepository.save(reconciliation);
        return report(current, reconciliation);
    }

    private void refreshSystemAmounts(Employee current, CarryoverReconciliation reconciliation, boolean persist) {
        FinanceSummary summary = financeReportContext.summary(current);
        Map<CarryoverReconciliationTheme, BigDecimal> amounts = new EnumMap<>(CarryoverReconciliationTheme.class);
        amounts.put(CarryoverReconciliationTheme.Receivable, value(summary.getOutstandingAmount()));
        amounts.put(CarryoverReconciliationTheme.Payable, value(summary.getUnpaidAmount()));
        amounts.put(CarryoverReconciliationTheme.Cash, value(summary.getCashBalance()));
        amounts.put(CarryoverReconciliationTheme.Cost, value(summary.getActualCost()));
        for (CarryoverReconciliationTheme theme : CarryoverReconciliationTheme.values()) {
            CarryoverReconciliationItem item = itemRepository
                    .findByCompany_ComIdAndReconciliation_ReconciliationIdAndTheme(
                            current.getCompany().getComId(), reconciliation.getReconciliationId(), theme)
                    .orElseGet(() -> newItem(current, reconciliation, theme));
            item.setSystemAmount(amounts.get(theme));
            item.setDifference(difference(item.getOriginalAmount(), item.getSystemAmount()));
            if (persist) itemRepository.save(item);
        }
    }

    private CarryoverReconciliationItem newItem(Employee current, CarryoverReconciliation reconciliation,
                                                CarryoverReconciliationTheme theme) {
        CarryoverReconciliationItem item = new CarryoverReconciliationItem();
        item.setCompany(current.getCompany());
        item.setReconciliation(reconciliation);
        item.setTheme(theme);
        item.setSystemAmount(BigDecimal.ZERO);
        item.setOriginalConfirmed(Boolean.FALSE);
        item.setDifferenceStatus(CarryoverDifferenceStatus.Pending);
        return item;
    }

    private void updateStatus(CarryoverReconciliation reconciliation) {
        if (reconciliation == null || reconciliation.getReconciliationId() == null) return;
        List<CarryoverReconciliationItem> items = itemRepository
                .findByCompany_ComIdAndReconciliation_ReconciliationIdOrderByThemeAsc(
                        reconciliation.getCompany().getComId(), reconciliation.getReconciliationId());
        boolean handling = items.stream().anyMatch(item -> value(item.getDifference()).signum() != 0
                && (item.getDifferenceStatus() == null
                || item.getDifferenceStatus() == CarryoverDifferenceStatus.Pending
                || item.getDifferenceStatus() == CarryoverDifferenceStatus.Handling));
        if (handling) {
            reconciliation.setStatus(CarryoverReconciliationStatus.DiffHandling);
        } else if (reconciliation.getStatus() == CarryoverReconciliationStatus.Draft) {
            reconciliation.setStatus(CarryoverReconciliationStatus.Pending);
        }
    }

    private CarryoverReconciliationReport report(Employee current, CarryoverReconciliation reconciliation) {
        List<CarryoverReconciliationItem> items = itemRepository
                .findByCompany_ComIdAndReconciliation_ReconciliationIdOrderByThemeAsc(
                        current.getCompany().getComId(), reconciliation.getReconciliationId());
        int unclassified = 0;
        int pending = 0;
        for (CarryoverReconciliationItem item : items) {
            if (value(item.getDifference()).signum() != 0 && item.getDifferenceType() == null) unclassified++;
            if (value(item.getDifference()).signum() != 0 && (item.getDifferenceStatus() == null
                    || item.getDifferenceStatus() == CarryoverDifferenceStatus.Pending
                    || item.getDifferenceStatus() == CarryoverDifferenceStatus.Handling)) pending++;
        }
        CarryoverReconciliationReport report = new CarryoverReconciliationReport();
        report.setReconciliationId(reconciliation.getReconciliationId());
        report.setBatchId(reconciliation.getBatch() == null ? null : reconciliation.getBatch().getBatchId());
        report.setAsOfDate(reconciliation.getAsOfDate());
        report.setPeriodStart(reconciliation.getPeriodStart());
        report.setPeriodEnd(reconciliation.getPeriodEnd());
        report.setStatus(reconciliation.getStatus());
        report.setRemark(reconciliation.getRemark());
        report.setItems(new ArrayList<>(items));
        report.setUnclassifiedCount(unclassified);
        report.setPendingCount(pending);
        return report;
    }

    private BigDecimal difference(BigDecimal original, BigDecimal system) {
        if (original == null) return null;
        return original.subtract(value(system));
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
