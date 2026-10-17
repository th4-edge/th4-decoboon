package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

/** 材料业务助手只读查询。HTTP 参数由 Provider 的 @AutoInject 字段承载。 */
@Note("材料业务助手查询")
public class MaterialAssistantQuery {

    @Note("自然语言问题")
    private String question;
    @Note("项目索引")
    private Long projectId;
    @Note("作业单索引")
    private Long sheetId;
    @Note("请求标识")
    private String requestId;
    @Note("幂等键")
    private String idempotencyKey;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getSheetId() { return sheetId; }
    public void setSheetId(Long sheetId) { this.sheetId = sheetId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
