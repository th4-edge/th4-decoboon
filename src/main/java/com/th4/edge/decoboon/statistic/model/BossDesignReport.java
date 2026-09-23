package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 企业负责人设计方案经营报告。 */
@Note("企业负责人设计经营报告")
public class BossDesignReport {

    private LocalDate asOf;
    private Integer pipelineCount;
    private Integer unassignedCount;
    private Integer requirementPendingCount;
    private Integer schemePendingCount;
    private Integer designingCount;
    private Integer quoteDraftCount;
    private Integer quoteSubmittedCount;
    private Integer confirmedCount;
    private Integer contractedCount;
    private Integer talkGeneratedCount;
    private Integer riskCount;
    private BigDecimal quoteAmount;
    private BigDecimal acceptedAmount;
    private List<BossDesignerLoad> designers = List.of();
    private List<BossDesignItem> items = List.of();

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public Integer getPipelineCount() { return pipelineCount; }
    public void setPipelineCount(Integer pipelineCount) { this.pipelineCount = pipelineCount; }
    public Integer getUnassignedCount() { return unassignedCount; }
    public void setUnassignedCount(Integer unassignedCount) { this.unassignedCount = unassignedCount; }
    public Integer getRequirementPendingCount() { return requirementPendingCount; }
    public void setRequirementPendingCount(Integer requirementPendingCount) { this.requirementPendingCount = requirementPendingCount; }
    public Integer getSchemePendingCount() { return schemePendingCount; }
    public void setSchemePendingCount(Integer schemePendingCount) { this.schemePendingCount = schemePendingCount; }
    public Integer getDesigningCount() { return designingCount; }
    public void setDesigningCount(Integer designingCount) { this.designingCount = designingCount; }
    public Integer getQuoteDraftCount() { return quoteDraftCount; }
    public void setQuoteDraftCount(Integer quoteDraftCount) { this.quoteDraftCount = quoteDraftCount; }
    public Integer getQuoteSubmittedCount() { return quoteSubmittedCount; }
    public void setQuoteSubmittedCount(Integer quoteSubmittedCount) { this.quoteSubmittedCount = quoteSubmittedCount; }
    public Integer getConfirmedCount() { return confirmedCount; }
    public void setConfirmedCount(Integer confirmedCount) { this.confirmedCount = confirmedCount; }
    public Integer getContractedCount() { return contractedCount; }
    public void setContractedCount(Integer contractedCount) { this.contractedCount = contractedCount; }
    public Integer getTalkGeneratedCount() { return talkGeneratedCount; }
    public void setTalkGeneratedCount(Integer talkGeneratedCount) { this.talkGeneratedCount = talkGeneratedCount; }
    public Integer getRiskCount() { return riskCount; }
    public void setRiskCount(Integer riskCount) { this.riskCount = riskCount; }
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    public BigDecimal getAcceptedAmount() { return acceptedAmount; }
    public void setAcceptedAmount(BigDecimal acceptedAmount) { this.acceptedAmount = acceptedAmount; }
    public List<BossDesignerLoad> getDesigners() { return designers; }
    public void setDesigners(List<BossDesignerLoad> designers) { this.designers = designers; }
    public List<BossDesignItem> getItems() { return items; }
    public void setItems(List<BossDesignItem> items) { this.items = items; }
}
