package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus;
import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 双轨对账详情：批次信息 + 四主题。 */
@Note("双轨对账详情")
public class CarryoverReconciliationReport {

    private Long reconciliationId;
    private Long batchId;
    private LocalDate asOfDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private CarryoverReconciliationStatus status;
    private String remark;
    private Integer unclassifiedCount = 0;
    private Integer pendingCount = 0;
    private Boolean complete = Boolean.FALSE;
    private List<String> blockers = new ArrayList<>();
    private List<CarryoverChecklistItem> checklist = new ArrayList<>();
    private List<CarryoverReconciliationItem> items = new ArrayList<>();

    public Long getReconciliationId() { return reconciliationId; }
    public void setReconciliationId(Long reconciliationId) { this.reconciliationId = reconciliationId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public CarryoverReconciliationStatus getStatus() { return status; }
    public void setStatus(CarryoverReconciliationStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getUnclassifiedCount() { return unclassifiedCount; }
    public void setUnclassifiedCount(Integer unclassifiedCount) { this.unclassifiedCount = unclassifiedCount; }
    public Integer getPendingCount() { return pendingCount; }
    public void setPendingCount(Integer pendingCount) { this.pendingCount = pendingCount; }
    public Boolean getComplete() { return complete; }
    public void setComplete(Boolean complete) { this.complete = complete; }
    public List<String> getBlockers() { return blockers; }
    public void setBlockers(List<String> blockers) { this.blockers = blockers == null ? new ArrayList<>() : blockers; }
    public List<CarryoverChecklistItem> getChecklist() { return checklist; }
    public void setChecklist(List<CarryoverChecklistItem> checklist) { this.checklist = checklist == null ? new ArrayList<>() : checklist; }
    public List<CarryoverReconciliationItem> getItems() { return items; }
    public void setItems(List<CarryoverReconciliationItem> items) { this.items = items == null ? new ArrayList<>() : items; }
}
