package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 企业负责人视角的客户报价谈单聚合。 */
@Note("企业负责人客户报价谈单")
public class BossQuoteCustomer {

    private Long custId;
    private String customerName;
    private String telNo;
    private Integer propertyCount;
    private Integer quoteCount;
    private Integer draftPropertyCount;
    private Integer submittedPropertyCount;
    private Integer rejectedPropertyCount;
    private Integer confirmedPropertyCount;
    private Integer talkPropertyCount;
    private Integer riskPropertyCount;
    private BigDecimal latestQuoteAmount;
    private BigDecimal acceptedAmount;
    private Long designerId;
    private String designerName;
    private String designerStoreName;
    private String nextAction;
    private String riskLevel;
    private LocalDateTime lastActivityAt;
    private List<BossQuoteProperty> properties = List.of();

    public Long getCustId() { return custId; }
    public void setCustId(Long custId) { this.custId = custId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    public Integer getPropertyCount() { return propertyCount; }
    public void setPropertyCount(Integer propertyCount) { this.propertyCount = propertyCount; }
    public Integer getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Integer quoteCount) { this.quoteCount = quoteCount; }
    public Integer getDraftPropertyCount() { return draftPropertyCount; }
    public void setDraftPropertyCount(Integer draftPropertyCount) { this.draftPropertyCount = draftPropertyCount; }
    public Integer getSubmittedPropertyCount() { return submittedPropertyCount; }
    public void setSubmittedPropertyCount(Integer submittedPropertyCount) { this.submittedPropertyCount = submittedPropertyCount; }
    public Integer getRejectedPropertyCount() { return rejectedPropertyCount; }
    public void setRejectedPropertyCount(Integer rejectedPropertyCount) { this.rejectedPropertyCount = rejectedPropertyCount; }
    public Integer getConfirmedPropertyCount() { return confirmedPropertyCount; }
    public void setConfirmedPropertyCount(Integer confirmedPropertyCount) { this.confirmedPropertyCount = confirmedPropertyCount; }
    public Integer getTalkPropertyCount() { return talkPropertyCount; }
    public void setTalkPropertyCount(Integer talkPropertyCount) { this.talkPropertyCount = talkPropertyCount; }
    public Integer getRiskPropertyCount() { return riskPropertyCount; }
    public void setRiskPropertyCount(Integer riskPropertyCount) { this.riskPropertyCount = riskPropertyCount; }
    public BigDecimal getLatestQuoteAmount() { return latestQuoteAmount; }
    public void setLatestQuoteAmount(BigDecimal latestQuoteAmount) { this.latestQuoteAmount = latestQuoteAmount; }
    public BigDecimal getAcceptedAmount() { return acceptedAmount; }
    public void setAcceptedAmount(BigDecimal acceptedAmount) { this.acceptedAmount = acceptedAmount; }
    public Long getDesignerId() { return designerId; }
    public void setDesignerId(Long designerId) { this.designerId = designerId; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
    public String getDesignerStoreName() { return designerStoreName; }
    public void setDesignerStoreName(String designerStoreName) { this.designerStoreName = designerStoreName; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public List<BossQuoteProperty> getProperties() { return properties; }
    public void setProperties(List<BossQuoteProperty> properties) { this.properties = properties; }
}
