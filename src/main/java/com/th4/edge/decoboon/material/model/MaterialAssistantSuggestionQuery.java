package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

/** 材料业务助手建议问题查询。HTTP 参数由 Provider 的 @AutoInject 字段承载。 */
@Note("材料业务助手建议问题查询")
public class MaterialAssistantSuggestionQuery {

    @Note("项目索引")
    private Long projectId;
    @Note("作业单索引")
    private Long sheetId;
    @Note("上下文类型：project/sheet/purchase/settlement/general")
    private String contextType;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getSheetId() { return sheetId; }
    public void setSheetId(Long sheetId) { this.sheetId = sheetId; }
    public String getContextType() { return contextType; }
    public void setContextType(String contextType) { this.contextType = contextType; }
}
