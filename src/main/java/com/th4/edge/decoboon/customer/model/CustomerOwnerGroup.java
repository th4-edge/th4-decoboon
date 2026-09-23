package com.th4.edge.decoboon.customer.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

/** 企业负责人客户负责人分组摘要。 */
@Note("客户负责人分组")
public class CustomerOwnerGroup {

    private Long ownerId;
    private String ownerName;
    private String storeName;
    private Integer customerCount = 0;
    private Integer riskCustomerCount = 0;
    private Integer pendingMeasureCount = 0;
    private Integer signedCustomerCount = 0;
    private BigDecimal quoteAmount = BigDecimal.ZERO;
    private BigDecimal signedAmount = BigDecimal.ZERO;

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getRiskCustomerCount() { return riskCustomerCount; }
    public void setRiskCustomerCount(Integer riskCustomerCount) { this.riskCustomerCount = riskCustomerCount; }
    public Integer getPendingMeasureCount() { return pendingMeasureCount; }
    public void setPendingMeasureCount(Integer pendingMeasureCount) { this.pendingMeasureCount = pendingMeasureCount; }
    public Integer getSignedCustomerCount() { return signedCustomerCount; }
    public void setSignedCustomerCount(Integer signedCustomerCount) { this.signedCustomerCount = signedCustomerCount; }
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    public BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
}
