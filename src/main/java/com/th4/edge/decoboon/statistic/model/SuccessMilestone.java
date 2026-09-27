package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.statistic.configure.MilestoneStatus;
import com.th4.edge.framework.core.annotation.Note;

/** 7/14/30 天用户成功里程碑。 */
@Note("用户成功里程碑")
public class SuccessMilestone {

    private String key;
    private String label;
    private String description;
    private int targetDay;
    private MilestoneStatus status = MilestoneStatus.Todo;
    private int count;
    private String actionLabel;
    private String actionPath;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getTargetDay() { return targetDay; }
    public void setTargetDay(int targetDay) { this.targetDay = targetDay; }
    public MilestoneStatus getStatus() { return status; }
    public void setStatus(MilestoneStatus status) { this.status = status; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
    public String getActionPath() { return actionPath; }
    public void setActionPath(String actionPath) { this.actionPath = actionPath; }
}
