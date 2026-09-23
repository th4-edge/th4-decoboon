package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 企业负责人报价谈单设计师经营分组。 */
@Note("报价谈单设计师分组")
public class BossQuoteDesignerGroup {

    private Long designerId;
    private String designerName;
    private String storeName;
    private Integer customerCount;
    private Integer riskCustomerCount;
    private Integer pendingConfirmCustomerCount;
    private Integer confirmedCustomerCount;
    private Integer quoteCount;
    private BigDecimal quoteAmount;
    private BigDecimal acceptedAmount;
    private LocalDateTime lastActivityAt;
    private List<BossQuoteCustomer> customers = List.of();

    public Long getDesignerId() { return designerId; }
    public void setDesignerId(Long designerId) { this.designerId = designerId; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getRiskCustomerCount() { return riskCustomerCount; }
    public void setRiskCustomerCount(Integer riskCustomerCount) { this.riskCustomerCount = riskCustomerCount; }
    public Integer getPendingConfirmCustomerCount() { return pendingConfirmCustomerCount; }
    public void setPendingConfirmCustomerCount(Integer pendingConfirmCustomerCount) { this.pendingConfirmCustomerCount = pendingConfirmCustomerCount; }
    public Integer getConfirmedCustomerCount() { return confirmedCustomerCount; }
    public void setConfirmedCustomerCount(Integer confirmedCustomerCount) { this.confirmedCustomerCount = confirmedCustomerCount; }
    public Integer getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Integer quoteCount) { this.quoteCount = quoteCount; }
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    public BigDecimal getAcceptedAmount() { return acceptedAmount; }
    public void setAcceptedAmount(BigDecimal acceptedAmount) { this.acceptedAmount = acceptedAmount; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public List<BossQuoteCustomer> getCustomers() { return customers; }
    public void setCustomers(List<BossQuoteCustomer> customers) { this.customers = customers; }
}
