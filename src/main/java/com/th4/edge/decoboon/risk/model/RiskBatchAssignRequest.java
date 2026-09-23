package com.th4.edge.decoboon.risk.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.List;

/** 批量指派入参。 */
@Note("风险批量指派入参")
public class RiskBatchAssignRequest {
    private List<Long> riskIds;
    private Long assigneeId;
    private String note;

    public List<Long> getRiskIds() { return riskIds; }
    public void setRiskIds(List<Long> riskIds) { this.riskIds = riskIds; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
