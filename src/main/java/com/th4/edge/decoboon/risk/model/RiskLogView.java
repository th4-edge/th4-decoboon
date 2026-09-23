package com.th4.edge.decoboon.risk.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDateTime;
import java.util.List;

/** 风险处置时间线行。 */
@Note("风险处置时间线行")
public class RiskLogView {

    private Long logId;
    private String action;
    private String actionLabel;
    private String fromStatusLabel;
    private String toStatusLabel;
    private String operatorName;
    private String note;
    private List<String> evidenceUrls;
    private LocalDateTime occurredAt;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
    public String getFromStatusLabel() { return fromStatusLabel; }
    public void setFromStatusLabel(String fromStatusLabel) { this.fromStatusLabel = fromStatusLabel; }
    public String getToStatusLabel() { return toStatusLabel; }
    public void setToStatusLabel(String toStatusLabel) { this.toStatusLabel = toStatusLabel; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<String> getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(List<String> evidenceUrls) { this.evidenceUrls = evidenceUrls; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
