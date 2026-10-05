package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.framework.core.annotation.Note;

/** 双轨对账完成度投影项。 */
@Note("对账完成度项")
public class CarryoverChecklistItem {

    private String key;
    private String label;
    private Boolean required = Boolean.TRUE;
    private Boolean completed = Boolean.FALSE;
    private String exception;

    public CarryoverChecklistItem() {}

    public CarryoverChecklistItem(String key, String label, boolean completed, String exception) {
        this.key = key;
        this.label = label;
        this.completed = completed;
        this.exception = exception;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public String getException() { return exception; }
    public void setException(String exception) { this.exception = exception; }
}
