package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverDifferenceStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverDifferenceType;
import com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationTheme;
import com.th4.edge.decoboon.carryover.model.CarryoverChecklistItem;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationAttachment;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationExport;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationItem;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationReport;
import com.th4.edge.decoboon.carryover.repository.CarryoverBatchRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverReconciliationAttachmentRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverReconciliationItemRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverReconciliationRepository;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 首月双轨对账：四个主题原账/装得清对照、差异处理、确认关闭与导出。 */
@Component
public class CarryoverReconciliationContext {

    private final CarryoverAccessContext accessContext;
    private final CarryoverBatchRepository batchRepository;
    private final CarryoverReconciliationRepository reconciliationRepository;
    private final CarryoverReconciliationItemRepository itemRepository;
    private final FinanceReportContext financeReportContext;
    private final CarryoverReconciliationAttachmentRepository attachmentRepository;

    public CarryoverReconciliationContext(CarryoverAccessContext accessContext,
                                          CarryoverBatchRepository batchRepository,
                                          CarryoverReconciliationRepository reconciliationRepository,
                                          CarryoverReconciliationItemRepository itemRepository,
                                          FinanceReportContext financeReportContext) {
        this(accessContext, batchRepository, reconciliationRepository, itemRepository, financeReportContext, null);
    }

    @Autowired
    public CarryoverReconciliationContext(CarryoverAccessContext accessContext,
                                          CarryoverBatchRepository batchRepository,
                                          CarryoverReconciliationRepository reconciliationRepository,
                                          CarryoverReconciliationItemRepository itemRepository,
                                          FinanceReportContext financeReportContext,
                                          CarryoverReconciliationAttachmentRepository attachmentRepository) {
        this.accessContext = accessContext;
        this.batchRepository = batchRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.itemRepository = itemRepository;
        this.financeReportContext = financeReportContext;
        this.attachmentRepository = attachmentRepository;
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
        CarryoverReconciliation reconciliation = require(current, reconciliationId);
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
        updateProgressStatus(reconciliation);
        reconciliationRepository.save(reconciliation);
        return report(current, reconciliation);
    }

    /** 状态流转：财务/BOSS 提交待确认 -> BOSS 确认 -> BOSS 关闭；未完成不得确认或关闭。 */
    @Transactional
    public CarryoverReconciliationReport updateStatus(Employee principal, Long reconciliationId,
                                                      CarryoverReconciliationStatus status, String remark) {
        Employee current = accessContext.requireFinance(principal);
        CarryoverReconciliation reconciliation = require(current, reconciliationId);
        if (status == null) {
            throw new DefinedException(new DefaultErrorCode("400", "对账状态不能为空"));
        }
        CarryoverReconciliationReport currentReport = report(current, reconciliation);
        if (!Boolean.TRUE.equals(currentReport.getComplete())
                && (status == CarryoverReconciliationStatus.Pending
                || status == CarryoverReconciliationStatus.Confirmed
                || status == CarryoverReconciliationStatus.Closed)) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "对账未完成：" + String.join("、", currentReport.getBlockers())));
        }
        if (status == CarryoverReconciliationStatus.Pending) {
            reconciliation.setStatus(CarryoverReconciliationStatus.Pending);
        } else if (status == CarryoverReconciliationStatus.Confirmed) {
            requireBoss(current);
            if (reconciliation.getStatus() != CarryoverReconciliationStatus.Pending) {
                throw new DefinedException(new DefaultErrorCode("400", "请先提交待确认后再确认"));
            }
            reconciliation.setStatus(CarryoverReconciliationStatus.Confirmed);
            reconciliation.setConfirmedBy(current);
            reconciliation.setConfirmedAt(LocalDateTime.now());
        } else if (status == CarryoverReconciliationStatus.Closed) {
            requireBoss(current);
            if (reconciliation.getStatus() != CarryoverReconciliationStatus.Confirmed) {
                throw new DefinedException(new DefaultErrorCode("400", "只有已确认的对账可以关闭"));
            }
            reconciliation.setStatus(CarryoverReconciliationStatus.Closed);
            reconciliation.setClosedAt(LocalDateTime.now());
        } else {
            throw new DefinedException(new DefaultErrorCode("400", "不支持的对账状态"));
        }
        if (remark != null) reconciliation.setRemark(remark.trim());
        reconciliationRepository.save(reconciliation);
        return report(current, reconciliation);
    }

    @Transactional(readOnly = true)
    public CarryoverReconciliationExport export(Employee principal, Long reconciliationId) {
        Employee current = accessContext.requireFinance(principal);
        CarryoverReconciliation reconciliation = require(current, reconciliationId);
        CarryoverReconciliationReport report = report(current, reconciliation);
        StringBuilder csv = new StringBuilder();
        csv.append("首月双轨对账表\n");
        csv.append("对账期间,").append(reconciliation.getPeriodStart()).append(",")
                .append(reconciliation.getPeriodEnd()).append('\n');
        csv.append("接管日,").append(reconciliation.getAsOfDate()).append('\n');
        csv.append("状态,").append(reconciliation.getStatus() == null ? "" : reconciliation.getStatus().getName()).append('\n');
        csv.append("主题,原账金额,装得清金额,差异,差异分类,差异状态,原因,处理动作\n");
        for (CarryoverReconciliationItem item : report.getItems()) {
            csv.append(safe(item.getTheme() == null ? null : item.getTheme().getName())).append(',')
                    .append(amount(item.getOriginalAmount())).append(',')
                    .append(amount(item.getSystemAmount())).append(',')
                    .append(amount(item.getDifference())).append(',')
                    .append(safe(item.getDifferenceType() == null ? null : item.getDifferenceType().getName())).append(',')
                    .append(safe(item.getDifferenceStatus() == null ? null : item.getDifferenceStatus().getName())).append(',')
                    .append(safe(item.getReason())).append(',')
                    .append(safe(item.getAction())).append('\n');
        }
        csv.append("\n未分类差异,").append(report.getUnclassifiedCount())
                .append(",待处理差异,").append(report.getPendingCount())
                .append(",是否完成,").append(Boolean.TRUE.equals(report.getComplete()) ? "是" : "否").append('\n');
        if (!report.getBlockers().isEmpty()) {
            csv.append("未完成项,").append(safe(String.join("、", report.getBlockers()))).append('\n');
        }
        CarryoverReconciliationExport export = new CarryoverReconciliationExport();
        export.setFileName("首月双轨对账-" + reconciliation.getPeriodEnd() + ".csv");
        export.setContentType("text/csv;charset=utf-8");
        export.setContent("\ufeff" + csv);
        return export;
    }

    @Transactional
    public CarryoverReconciliationAttachment saveAttachment(Employee principal, Long reconciliationId,
                                                            String category, String fileUrl, String originalName) {
        Employee current = accessContext.requireFinance(principal);
        CarryoverReconciliation reconciliation = require(current, reconciliationId);
        CarryoverReconciliationAttachment attachment = new CarryoverReconciliationAttachment();
        attachment.setCompany(current.getCompany());
        attachment.setReconciliation(reconciliation);
        attachment.setCategory(category == null ? "Other" : category.trim());
        attachment.setFileUrl(fileUrl);
        attachment.setOriginalName(originalName);
        attachment.setUploadedBy(current);
        return attachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public List<CarryoverReconciliationAttachment> attachments(Employee principal, Long reconciliationId) {
        Employee current = accessContext.requireFinance(principal);
        require(current, reconciliationId);
        return attachmentRepository
                .findByCompany_ComIdAndReconciliation_ReconciliationIdOrderByAttachmentIdAsc(
                        current.getCompany().getComId(), reconciliationId);
    }

    @Transactional
    public void deleteAttachment(Employee principal, Long attachmentId) {
        Employee current = accessContext.requireFinance(principal);
        CarryoverReconciliationAttachment attachment = attachmentRepository.findById(attachmentId)
                .filter(item -> item.getCompany() != null
                        && current.getCompany().getComId().equals(item.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "附件不存在或无权访问")));
        attachmentRepository.delete(attachment);
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

    private void updateProgressStatus(CarryoverReconciliation reconciliation) {
        if (reconciliation == null || reconciliation.getReconciliationId() == null) return;
        List<CarryoverReconciliationItem> items = items(reconciliation);
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
        List<CarryoverReconciliationItem> items = items(reconciliation);
        int unclassified = 0;
        int pending = 0;
        List<CarryoverChecklistItem> checklist = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        for (CarryoverReconciliationItem item : items) {
            boolean classified = value(item.getDifference()).signum() == 0 || item.getDifferenceType() != null;
            boolean resolved = value(item.getDifference()).signum() == 0
                    || item.getDifferenceStatus() == CarryoverDifferenceStatus.Resolved
                    || item.getDifferenceStatus() == CarryoverDifferenceStatus.Accepted;
            boolean confirmed = Boolean.TRUE.equals(item.getOriginalConfirmed()) && item.getOriginalAmount() != null;
            if (!classified) unclassified++;
            if (!resolved) pending++;
            String key = item.getTheme().name().toLowerCase();
            String label = item.getTheme().getName();
            String exception = !confirmed ? "原账金额未确认" : (!classified ? "差异未分类" : (!resolved ? "差异未处理" : null));
            checklist.add(new CarryoverChecklistItem(key, label, confirmed && classified && resolved, exception));
            if (exception != null) blockers.add(label + "：" + exception);
        }
        boolean complete = blockers.isEmpty();
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
        report.setComplete(complete);
        report.setBlockers(blockers);
        report.setChecklist(checklist);
        return report;
    }

    private List<CarryoverReconciliationItem> items(CarryoverReconciliation reconciliation) {
        return itemRepository.findByCompany_ComIdAndReconciliation_ReconciliationIdOrderByThemeAsc(
                reconciliation.getCompany().getComId(), reconciliation.getReconciliationId());
    }

    private CarryoverReconciliation require(Employee current, Long reconciliationId) {
        return reconciliationRepository
                .findByCompany_ComIdAndReconciliationId(current.getCompany().getComId(), reconciliationId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "对账批次不存在或无权访问")));
    }

    private void requireBoss(Employee current) {
        if (current.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人可以确认或关闭对账"));
        }
    }

    private BigDecimal difference(BigDecimal original, BigDecimal system) {
        if (original == null) return null;
        return original.subtract(value(system));
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String amount(BigDecimal value) {
        return value == null ? "" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
