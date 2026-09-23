package com.th4.edge.decoboon.risk.model;

import com.th4.edge.framework.core.annotation.Note;

/** 风险指派/认领入参。 */
@Note("风险指派入参")
public class RiskAssignRequest {
    private Long riskId;
    @Note("受理人工号；认领时可空（认领不改变受理人）")
    private Long assigneeId;
    @Note("指派说明；认领时可为空")
    private String note;

    public Long getRiskId() { return riskId; }
    public void setRiskId(Long riskId) { this.riskId = riskId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
