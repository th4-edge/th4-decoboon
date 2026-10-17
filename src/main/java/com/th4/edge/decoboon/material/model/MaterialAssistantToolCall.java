package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

/** 助手只读工具调用记录。 */
@Note("材料助手工具调用")
public class MaterialAssistantToolCall {

    @Note("工具名称")
    private String name;
    @Note("Provider/来源")
    private String provider;
    @Note("调用状态")
    private String status;
    @Note("数据范围")
    private String scope;
    @Note("是否只读")
    private boolean readOnly = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
}
