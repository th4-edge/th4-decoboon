package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 企业负责人报价谈单客户经营报告。 */
@Note("企业负责人报价谈单报告")
public class BossQuoteReport {

    private LocalDate asOf;
    private Integer customerCount;
    private Integer activeCustomerCount;
    private Integer pendingConfirmCustomerCount;
    private Integer confirmedCustomerCount;
    private Integer riskCustomerCount;
    private Integer talkGeneratedCustomerCount;
    private Integer quoteCount;
    private BigDecimal latestQuoteAmount;
    private BigDecimal acceptedAmount;
    private List<BossQuoteCustomer> customers = List.of();
    private List<BossQuoteDesignerGroup> designerGroups = List.of();

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getActiveCustomerCount() { return activeCustomerCount; }
    public void setActiveCustomerCount(Integer activeCustomerCount) { this.activeCustomerCount = activeCustomerCount; }
    public Integer getPendingConfirmCustomerCount() { return pendingConfirmCustomerCount; }
    public void setPendingConfirmCustomerCount(Integer pendingConfirmCustomerCount) { this.pendingConfirmCustomerCount = pendingConfirmCustomerCount; }
    public Integer getConfirmedCustomerCount() { return confirmedCustomerCount; }
    public void setConfirmedCustomerCount(Integer confirmedCustomerCount) { this.confirmedCustomerCount = confirmedCustomerCount; }
    public Integer getRiskCustomerCount() { return riskCustomerCount; }
    public void setRiskCustomerCount(Integer riskCustomerCount) { this.riskCustomerCount = riskCustomerCount; }
    public Integer getTalkGeneratedCustomerCount() { return talkGeneratedCustomerCount; }
    public void setTalkGeneratedCustomerCount(Integer talkGeneratedCustomerCount) { this.talkGeneratedCustomerCount = talkGeneratedCustomerCount; }
    public Integer getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Integer quoteCount) { this.quoteCount = quoteCount; }
    public BigDecimal getLatestQuoteAmount() { return latestQuoteAmount; }
    public void setLatestQuoteAmount(BigDecimal latestQuoteAmount) { this.latestQuoteAmount = latestQuoteAmount; }
    public BigDecimal getAcceptedAmount() { return acceptedAmount; }
    public void setAcceptedAmount(BigDecimal acceptedAmount) { this.acceptedAmount = acceptedAmount; }
    public List<BossQuoteCustomer> getCustomers() { return customers; }
    public void setCustomers(List<BossQuoteCustomer> customers) { this.customers = customers; }
    public List<BossQuoteDesignerGroup> getDesignerGroups() { return designerGroups; }
    public void setDesignerGroups(List<BossQuoteDesignerGroup> designerGroups) { this.designerGroups = designerGroups; }
}
