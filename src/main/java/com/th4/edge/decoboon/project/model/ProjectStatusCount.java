package com.th4.edge.decoboon.project.model;

import com.th4.edge.framework.core.annotation.Note;

/** 项目状态计数。 */
@Note("项目状态计数")
public class ProjectStatusCount {

    private String key;
    private String label;
    private Integer count = 0;

    public ProjectStatusCount() { }

    public ProjectStatusCount(String key, String label, Integer count) {
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
