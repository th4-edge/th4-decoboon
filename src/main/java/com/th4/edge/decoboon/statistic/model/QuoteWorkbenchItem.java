package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设计师报价工作台列表项。
 * 聚合"需要编制/跟进报价确认"的客户房屋，按设计师维度过滤。
 */
@Note("报价工作台列表项")
public class QuoteWorkbenchItem {

    private Long custId;
    private Long propertyId;
    private String customerName;
    private String telNo;
    private String community;
    private BigDecimal area;
    private String customerStatus;
    private String customerStatusName;

    private Long quoteId;
    private String quoteNo;
    private Integer version;
    private BigDecimal amount;
    /** 报价状态：Draft/Submitted/Rejected/Accepted */
    private String quoteStatus;
    private String quoteStatusName;

    /** 报价创建/提交时间 */
    private LocalDateTime submittedAt;
    /** 客户最后反馈时间（被拒绝/接受的时间） */
    private LocalDateTime lastFeedbackAt;
    /** 驳回后重新提交形成的调整次数 */
    private Integer adjustmentCount;
    /** 在当前报价状态停留天数 */
    private Long stayDays;

    public Long getCustId() { return custId; }
    public void setCustId(Long custId) { this.custId = custId; }
    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public String getCustomerStatus() { return customerStatus; }
    public void setCustomerStatus(String customerStatus) { this.customerStatus = customerStatus; }
    public String getCustomerStatusName() { return customerStatusName; }
    public void setCustomerStatusName(String customerStatusName) { this.customerStatusName = customerStatusName; }
    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public String getQuoteNo() { return quoteNo; }
    public void setQuoteNo(String quoteNo) { this.quoteNo = quoteNo; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getQuoteStatus() { return quoteStatus; }
    public void setQuoteStatus(String quoteStatus) { this.quoteStatus = quoteStatus; }
    public String getQuoteStatusName() { return quoteStatusName; }
    public void setQuoteStatusName(String quoteStatusName) { this.quoteStatusName = quoteStatusName; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getLastFeedbackAt() { return lastFeedbackAt; }
    public void setLastFeedbackAt(LocalDateTime lastFeedbackAt) { this.lastFeedbackAt = lastFeedbackAt; }
    public Integer getAdjustmentCount() { return adjustmentCount; }
    public void setAdjustmentCount(Integer adjustmentCount) { this.adjustmentCount = adjustmentCount; }
    public Long getStayDays() { return stayDays; }
    public void setStayDays(Long stayDays) { this.stayDays = stayDays; }
}
