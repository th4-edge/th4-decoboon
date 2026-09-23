package com.th4.edge.decoboon.customer.model;

import com.th4.edge.framework.core.annotation.Note;

/** 客户阶段计数。 */
@Note("客户阶段计数")
public class CustomerStageCount {
    private String key;
    private String label;
    private Integer count = 0;

    public CustomerStageCount() { }

    public CustomerStageCount(String key, String label, Integer count) {
        this.key = key;
        this.label = label;
        this.count = count;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}
