package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.List;

/** 量房执行设计师分组。 */
@Note("量房执行设计师分组")
public class BossMeasureDesignerGroup {

    private Long designerId;
    private String designerName;
    private String storeName;
    private Integer scheduledCount;
    private Integer completedCount;
    private Integer overdue4hCount;
    private List<BossMeasureItem> items = List.of();

    public Long getDesignerId() { return designerId; }
    public void setDesignerId(Long designerId) { this.designerId = designerId; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Integer getScheduledCount() { return scheduledCount; }
    public void setScheduledCount(Integer scheduledCount) { this.scheduledCount = scheduledCount; }
    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
    public Integer getOverdue4hCount() { return overdue4hCount; }
    public void setOverdue4hCount(Integer overdue4hCount) { this.overdue4hCount = overdue4hCount; }
    public List<BossMeasureItem> getItems() { return items; }
    public void setItems(List<BossMeasureItem> items) { this.items = items; }
}
