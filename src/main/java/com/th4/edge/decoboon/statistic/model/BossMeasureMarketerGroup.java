package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

/** 量房预约业务员经营分组。 */
@Note("量房预约业务员分组")
public class BossMeasureMarketerGroup {

    private Long marketerId;
    private String marketerName;
    private String storeName;
    private Integer itemCount;
    private Integer notScheduledCount;
    private Integer scheduledCount;
    private Integer completedCount;
    private Integer overdue4hCount;
    private Integer unassignedCount;
    private BigDecimal appointmentRate;
    private BigDecimal completionRate;
    private BigDecimal overdueRate;
    private List<BossMeasureItem> items = List.of();

    public Long getMarketerId() { return marketerId; }
    public void setMarketerId(Long marketerId) { this.marketerId = marketerId; }
    public String getMarketerName() { return marketerName; }
    public void setMarketerName(String marketerName) { this.marketerName = marketerName; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Integer getNotScheduledCount() { return notScheduledCount; }
    public void setNotScheduledCount(Integer notScheduledCount) { this.notScheduledCount = notScheduledCount; }
    public Integer getScheduledCount() { return scheduledCount; }
    public void setScheduledCount(Integer scheduledCount) { this.scheduledCount = scheduledCount; }
    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
    public Integer getOverdue4hCount() { return overdue4hCount; }
    public void setOverdue4hCount(Integer overdue4hCount) { this.overdue4hCount = overdue4hCount; }
    public Integer getUnassignedCount() { return unassignedCount; }
    public void setUnassignedCount(Integer unassignedCount) { this.unassignedCount = unassignedCount; }
    public BigDecimal getAppointmentRate() { return appointmentRate; }
    public void setAppointmentRate(BigDecimal appointmentRate) { this.appointmentRate = appointmentRate; }
    public BigDecimal getCompletionRate() { return completionRate; }
    public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }
    public BigDecimal getOverdueRate() { return overdueRate; }
    public void setOverdueRate(BigDecimal overdueRate) { this.overdueRate = overdueRate; }
    public List<BossMeasureItem> getItems() { return items; }
    public void setItems(List<BossMeasureItem> items) { this.items = items; }
}
