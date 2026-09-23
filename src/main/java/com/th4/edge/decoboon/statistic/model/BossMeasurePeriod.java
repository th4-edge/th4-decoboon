package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

/** 量房经营指标的日期窗口增量。 */
@Note("量房经营日期窗口")
public class BossMeasurePeriod {

    private String key;
    private String label;
    private Integer scheduledCount = 0;
    private Integer completedCount = 0;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getScheduledCount() { return scheduledCount; }
    public void setScheduledCount(Integer scheduledCount) { this.scheduledCount = scheduledCount; }
    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
}
