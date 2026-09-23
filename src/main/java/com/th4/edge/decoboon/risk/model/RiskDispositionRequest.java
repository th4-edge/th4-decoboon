package com.th4.edge.decoboon.risk.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.List;

/**
 * 风险处置入参（处置记录 / 催办 / 关闭 / 重开共用）。
 *
 * <p>关闭必须填 {@code result}；重开必须填 {@code reason}（用户 2026-09-23 裁决）。</p>
 */
@Note("风险处置入参")
public class RiskDispositionRequest {
    private Long riskId;
    @Note("处置说明 / 处置结果")
    private String note;
    @Note("重开原因")
    private String reason;
    @Note("关闭时的处置结果，等价于 note，便于前端语义区分")
    private String result;
    @Note("处置凭证（选填，用户裁决不强制）")
    private List<String> evidenceUrls;

    @Note("true=催办受理人；false/空=写处置说明")
    private Boolean urgent;

    public Long getRiskId() { return riskId; }
    public void setRiskId(Long riskId) { this.riskId = riskId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public List<String> getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(List<String> evidenceUrls) { this.evidenceUrls = evidenceUrls; }
    public Boolean getUrgent() { return urgent; }
    public void setUrgent(Boolean urgent) { this.urgent = urgent; }
}
