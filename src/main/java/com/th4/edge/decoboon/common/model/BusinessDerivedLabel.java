package com.th4.edge.decoboon.common.model;

import com.th4.edge.framework.core.annotation.Note;

/** 业务状态视图的派生标签。 */
@Note("业务状态派生标签")
public class BusinessDerivedLabel {
    private String key;
    private String label;
    private String tone;

    public BusinessDerivedLabel() {}

    public BusinessDerivedLabel(String key, String label, String tone) {
        this.key = key;
        this.label = label;
        this.tone = tone;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
}
