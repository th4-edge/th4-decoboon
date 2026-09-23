package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

/** 量房预约客户来源分组。 */
@Note("量房预约客户来源分组")
public class BossMeasureSourceGroup {

    private String source;
    private String sourceName;
    private Integer itemCount;
    private Integer scheduledCount;
    private Integer completedCount;
    private Integer handoffCount;
    private BigDecimal appointmentRate;
    private BigDecimal completionRate;
    private BigDecimal handoffRate;
    private List<BossMeasureItem> items = List.of();

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Integer getScheduledCount() { return scheduledCount; }
    public void setScheduledCount(Integer scheduledCount) { this.scheduledCount = scheduledCount; }
    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
    public Integer getHandoffCount() { return handoffCount; }
    public void setHandoffCount(Integer handoffCount) { this.handoffCount = handoffCount; }
    public BigDecimal getAppointmentRate() { return appointmentRate; }
    public void setAppointmentRate(BigDecimal appointmentRate) { this.appointmentRate = appointmentRate; }
    public BigDecimal getCompletionRate() { return completionRate; }
    public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }
    public BigDecimal getHandoffRate() { return handoffRate; }
    public void setHandoffRate(BigDecimal handoffRate) { this.handoffRate = handoffRate; }
    public List<BossMeasureItem> getItems() { return items; }
    public void setItems(List<BossMeasureItem> items) { this.items = items; }
}
