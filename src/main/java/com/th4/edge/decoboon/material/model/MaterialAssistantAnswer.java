package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.ArrayList;
import java.util.List;

/** 材料业务助手只读回答。 */
@Note("材料业务助手回答")
public class MaterialAssistantAnswer {

    @Note("原始问题")
    private String question;
    @Note("识别意图")
    private String intent;
    @Note("回答类型：EXPLANATION/LIVE_DATA/PERMISSION_DENIED/REFUSAL/NO_EVIDENCE")
    private String answerType;
    @Note("回答正文")
    private String answer;
    @Note("置信度")
    private double confidence;
    @Note("是否只读")
    private boolean readOnly = true;
    @Note("证据引用")
    private List<MaterialAssistantCitation> citations = new ArrayList<>();
    @Note("只读工具调用")
    private List<MaterialAssistantToolCall> toolCalls = new ArrayList<>();
    @Note("下一步")
    private List<String> nextActions = new ArrayList<>();
    @Note("建议追问")
    private List<String> suggestedQuestions = new ArrayList<>();
    @Note("审计记录索引")
    private Long auditId;
    @Note("审计状态")
    private String auditStatus;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getAnswerType() { return answerType; }
    public void setAnswerType(String answerType) { this.answerType = answerType; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
    public List<MaterialAssistantCitation> getCitations() { return citations; }
    public void setCitations(List<MaterialAssistantCitation> citations) { this.citations = citations; }
    public List<MaterialAssistantToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<MaterialAssistantToolCall> toolCalls) { this.toolCalls = toolCalls; }
    public List<String> getNextActions() { return nextActions; }
    public void setNextActions(List<String> nextActions) { this.nextActions = nextActions; }
    public List<String> getSuggestedQuestions() { return suggestedQuestions; }
    public void setSuggestedQuestions(List<String> suggestedQuestions) { this.suggestedQuestions = suggestedQuestions; }
    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }
    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(String auditStatus) { this.auditStatus = auditStatus; }
}
