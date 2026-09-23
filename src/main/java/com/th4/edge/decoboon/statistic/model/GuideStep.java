package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.statistic.configure.GuideStepStatus;
import com.th4.edge.framework.core.annotation.Note;

/** 启动引导步骤投影。 */
@Note("启动引导步骤")
public class GuideStep {

    private String key;
    private String label;
    private String description;
    private boolean required;
    private GuideStepStatus status = GuideStepStatus.Unknown;
    private Integer count;
    private String sourceProvider;
    private String completionPredicate;
    private String action;
    private String actionLabel;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public GuideStepStatus getStatus() { return status; }
    public void setStatus(GuideStepStatus status) { this.status = status; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public String getSourceProvider() { return sourceProvider; }
    public void setSourceProvider(String sourceProvider) { this.sourceProvider = sourceProvider; }
    public String getCompletionPredicate() { return completionPredicate; }
    public void setCompletionPredicate(String completionPredicate) { this.completionPredicate = completionPredicate; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
}
