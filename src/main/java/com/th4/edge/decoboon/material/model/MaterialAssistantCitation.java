package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

/** 助手证据引用。 */
@Note("材料助手证据引用")
public class MaterialAssistantCitation {

    @Note("引用类型")
    private String type;
    @Note("引用编号")
    private String id;
    @Note("引用名称")
    private String label;
    @Note("引用说明")
    private String detail;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
