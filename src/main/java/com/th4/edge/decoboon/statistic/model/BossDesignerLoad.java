package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 企业负责人视角的设计师设计负荷。 */
@Note("企业负责人设计负荷")
public class BossDesignerLoad {

    private Long designerId;
    private String designerName;
    private String storeName;
    private Integer customerCount;
    private Integer schemeCount;
    private Integer quoteCount;
    private Integer talkCount;
    private Integer confirmedCount;
    private Integer riskCount;
    private BigDecimal quoteAmount;
    private BigDecimal acceptedAmount;
    private LocalDateTime lastUpdateAt;

    public Long getDesignerId() { return designerId; }
    public void setDesignerId(Long designerId) { this.designerId = designerId; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getSchemeCount() { return schemeCount; }
    public void setSchemeCount(Integer schemeCount) { this.schemeCount = schemeCount; }
    public Integer getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Integer quoteCount) { this.quoteCount = quoteCount; }
    public Integer getTalkCount() { return talkCount; }
    public void setTalkCount(Integer talkCount) { this.talkCount = talkCount; }
    public Integer getConfirmedCount() { return confirmedCount; }
    public void setConfirmedCount(Integer confirmedCount) { this.confirmedCount = confirmedCount; }
    public Integer getRiskCount() { return riskCount; }
    public void setRiskCount(Integer riskCount) { this.riskCount = riskCount; }
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    public BigDecimal getAcceptedAmount() { return acceptedAmount; }
    public void setAcceptedAmount(BigDecimal acceptedAmount) { this.acceptedAmount = acceptedAmount; }
    public LocalDateTime getLastUpdateAt() { return lastUpdateAt; }
    public void setLastUpdateAt(LocalDateTime lastUpdateAt) { this.lastUpdateAt = lastUpdateAt; }
}
