package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 企业负责人量房经营报告。 */
@Note("企业负责人量房经营报告")
public class BossMeasureReport {

    private LocalDateTime asOf;
    private Integer itemCount;
    private Integer todayCount;
    private Integer riskCount;
    private Integer scheduledCount;
    private BigDecimal appointmentRate;
    private Integer completedCount;
    private BigDecimal completionRate;
    private Integer overdue4hCount;
    private BigDecimal overdueRate;
    private Integer handoffCount;
    private BigDecimal handoffRate;
    private Integer unassignedCount;
    private Integer handoff48hCount;
    private List<BossMeasurePeriod> periods = List.of();
    private List<BossMeasureItem> items = List.of();
    private List<BossMeasureMarketerGroup> marketerGroups = List.of();
    private List<BossMeasureSourceGroup> sourceGroups = List.of();
    private List<BossMeasureDesignerGroup> designerGroups = List.of();

    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Integer getTodayCount() { return todayCount; }
    public void setTodayCount(Integer todayCount) { this.todayCount = todayCount; }
    public Integer getRiskCount() { return riskCount; }
    public void setRiskCount(Integer riskCount) { this.riskCount = riskCount; }
    public Integer getScheduledCount() { return scheduledCount; }
    public void setScheduledCount(Integer scheduledCount) { this.scheduledCount = scheduledCount; }
    public BigDecimal getAppointmentRate() { return appointmentRate; }
    public void setAppointmentRate(BigDecimal appointmentRate) { this.appointmentRate = appointmentRate; }
    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
    public BigDecimal getCompletionRate() { return completionRate; }
    public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }
    public Integer getOverdue4hCount() { return overdue4hCount; }
    public void setOverdue4hCount(Integer overdue4hCount) { this.overdue4hCount = overdue4hCount; }
    public BigDecimal getOverdueRate() { return overdueRate; }
    public void setOverdueRate(BigDecimal overdueRate) { this.overdueRate = overdueRate; }
    public Integer getHandoffCount() { return handoffCount; }
    public void setHandoffCount(Integer handoffCount) { this.handoffCount = handoffCount; }
    public BigDecimal getHandoffRate() { return handoffRate; }
    public void setHandoffRate(BigDecimal handoffRate) { this.handoffRate = handoffRate; }
    public Integer getUnassignedCount() { return unassignedCount; }
    public void setUnassignedCount(Integer unassignedCount) { this.unassignedCount = unassignedCount; }
    public Integer getHandoff48hCount() { return handoff48hCount; }
    public void setHandoff48hCount(Integer handoff48hCount) { this.handoff48hCount = handoff48hCount; }
    public List<BossMeasurePeriod> getPeriods() { return periods; }
    public void setPeriods(List<BossMeasurePeriod> periods) {
        this.periods = periods == null ? List.of() : List.copyOf(periods);
    }
    public List<BossMeasureItem> getItems() { return items; }
    public void setItems(List<BossMeasureItem> items) { this.items = items; }
    public List<BossMeasureMarketerGroup> getMarketerGroups() { return marketerGroups; }
    public void setMarketerGroups(List<BossMeasureMarketerGroup> marketerGroups) { this.marketerGroups = marketerGroups; }
    public List<BossMeasureSourceGroup> getSourceGroups() { return sourceGroups; }
    public void setSourceGroups(List<BossMeasureSourceGroup> sourceGroups) { this.sourceGroups = sourceGroups; }
    public List<BossMeasureDesignerGroup> getDesignerGroups() { return designerGroups; }
    public void setDesignerGroups(List<BossMeasureDesignerGroup> designerGroups) { this.designerGroups = designerGroups; }
}
