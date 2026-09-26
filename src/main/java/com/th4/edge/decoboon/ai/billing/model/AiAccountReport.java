package com.th4.edge.decoboon.ai.billing.model;

import com.th4.edge.decoboon.ai.billing.configure.AiAccountStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.util.ArrayList;
import java.util.List;

/** 企业 AI 点数账户摘要。 */
@Note("AI资源账户摘要")
public class AiAccountReport {

    @Unit("点") private Long totalGranted = 0L;
    @Unit("点") private Long availableUnits = 0L;
    @Unit("点") private Long reservedUnits = 0L;
    @Unit("点") private Long consumedUnits = 0L;
    @Unit("点") private Long refundedUnits = 0L;
    @Unit("点") private Long adjustedUnits = 0L;
    private Long balanceVersion = 0L;
    private AiAccountStatus status = AiAccountStatus.Active;
    private Boolean formalEnabled = Boolean.FALSE;
    private List<AiResourceLedger> recentLedgers = new ArrayList<>();

    public Long getTotalGranted() { return totalGranted; }
    public void setTotalGranted(Long totalGranted) { this.totalGranted = totalGranted; }
    public Long getAvailableUnits() { return availableUnits; }
    public void setAvailableUnits(Long availableUnits) { this.availableUnits = availableUnits; }
    public Long getReservedUnits() { return reservedUnits; }
    public void setReservedUnits(Long reservedUnits) { this.reservedUnits = reservedUnits; }
    public Long getConsumedUnits() { return consumedUnits; }
    public void setConsumedUnits(Long consumedUnits) { this.consumedUnits = consumedUnits; }
    public Long getRefundedUnits() { return refundedUnits; }
    public void setRefundedUnits(Long refundedUnits) { this.refundedUnits = refundedUnits; }
    public Long getAdjustedUnits() { return adjustedUnits; }
    public void setAdjustedUnits(Long adjustedUnits) { this.adjustedUnits = adjustedUnits; }
    public Long getBalanceVersion() { return balanceVersion; }
    public void setBalanceVersion(Long balanceVersion) { this.balanceVersion = balanceVersion; }
    public AiAccountStatus getStatus() { return status; }
    public void setStatus(AiAccountStatus status) { this.status = status; }
    public Boolean getFormalEnabled() { return formalEnabled; }
    public void setFormalEnabled(Boolean formalEnabled) { this.formalEnabled = formalEnabled; }
    public List<AiResourceLedger> getRecentLedgers() { return recentLedgers; }
    public void setRecentLedgers(List<AiResourceLedger> recentLedgers) { this.recentLedgers = recentLedgers; }
}
