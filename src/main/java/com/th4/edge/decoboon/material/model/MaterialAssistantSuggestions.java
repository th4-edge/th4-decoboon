package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.ArrayList;
import java.util.List;

/** 材料业务助手初始建议问题。只来自发布蓝图对应的黄金问题，不新增业务事实。 */
@Note("材料业务助手建议问题")
public class MaterialAssistantSuggestions {

    @Note("上下文类型")
    private String contextType;
    @Note("建议问题")
    private List<String> suggestions = new ArrayList<>();
    @Note("是否只读")
    private boolean readOnly = true;

    public String getContextType() { return contextType; }
    public void setContextType(String contextType) { this.contextType = contextType; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
}
