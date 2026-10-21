package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设计师设计工作台列表项。
 * 聚合"需要出设计方案"的客户房屋，按设计师维度过滤。
 */
@Note("设计工作台列表项")
public class DesignWorkbenchItem {

    private Long custId;
    private Long propertyId;
    private String customerName;
    private String telNo;
    private String community;
    private String addr;
    private BigDecimal area;
    private String customerStatus;
    private String customerStatusName;

    /** 量房时间，null 表示未量房 */
    private LocalDateTime measureAt;
    /** 待量房预约时间 */
    private LocalDateTime pendingMeasureAt;

    /** 设计状态：Pending(待设计) / Doing(设计中) / Done(方案已完成) */
    private String designStatus;
    private String designStatusName;
    /** 设计方案数量 */
    private Integer schemeCount;
    /** 是否录入客户诉求 */
    private Boolean hasRequirement;
    /** 最近设计更新时间 */
    private LocalDateTime lastDesignAt;

    /** 最近方案索引 */
    private Long latestDesignId;
    /** 最近方案名称 */
    private String latestDesignName;
    /** 最近方案状态名称 */
    private String latestDesignStatusName;
    /** 方案计算价：当前设计分项销售金额合计 */
    private BigDecimal calculatedAmount;
    /** 当前正式报价金额 */
    private BigDecimal currentQuoteAmount;
    /** 当前正式报价编号 */
    private String currentQuoteNo;
    /** 当前正式报价状态名称 */
    private String currentQuoteStatusName;
    /** 列表主价格来源：Quote / Calculated / Unavailable */
    private String priceSource;

    /** 在当前设计阶段停留天数 */
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
    public String getAddr() { return addr; }
    public void setAddr(String addr) { this.addr = addr; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public String getCustomerStatus() { return customerStatus; }
    public void setCustomerStatus(String customerStatus) { this.customerStatus = customerStatus; }
    public String getCustomerStatusName() { return customerStatusName; }
    public void setCustomerStatusName(String customerStatusName) { this.customerStatusName = customerStatusName; }
    public LocalDateTime getMeasureAt() { return measureAt; }
    public void setMeasureAt(LocalDateTime measureAt) { this.measureAt = measureAt; }
    public LocalDateTime getPendingMeasureAt() { return pendingMeasureAt; }
    public void setPendingMeasureAt(LocalDateTime pendingMeasureAt) { this.pendingMeasureAt = pendingMeasureAt; }
    public String getDesignStatus() { return designStatus; }
    public void setDesignStatus(String designStatus) { this.designStatus = designStatus; }
    public String getDesignStatusName() { return designStatusName; }
    public void setDesignStatusName(String designStatusName) { this.designStatusName = designStatusName; }
    public Integer getSchemeCount() { return schemeCount; }
    public void setSchemeCount(Integer schemeCount) { this.schemeCount = schemeCount; }
    public Boolean getHasRequirement() { return hasRequirement; }
    public void setHasRequirement(Boolean hasRequirement) { this.hasRequirement = hasRequirement; }
    public LocalDateTime getLastDesignAt() { return lastDesignAt; }
    public void setLastDesignAt(LocalDateTime lastDesignAt) { this.lastDesignAt = lastDesignAt; }
    public Long getLatestDesignId() { return latestDesignId; }
    public void setLatestDesignId(Long latestDesignId) { this.latestDesignId = latestDesignId; }
    public String getLatestDesignName() { return latestDesignName; }
    public void setLatestDesignName(String latestDesignName) { this.latestDesignName = latestDesignName; }
    public String getLatestDesignStatusName() { return latestDesignStatusName; }
    public void setLatestDesignStatusName(String latestDesignStatusName) { this.latestDesignStatusName = latestDesignStatusName; }
    public BigDecimal getCalculatedAmount() { return calculatedAmount; }
    public void setCalculatedAmount(BigDecimal calculatedAmount) { this.calculatedAmount = calculatedAmount; }
    public BigDecimal getCurrentQuoteAmount() { return currentQuoteAmount; }
    public void setCurrentQuoteAmount(BigDecimal currentQuoteAmount) { this.currentQuoteAmount = currentQuoteAmount; }
    public String getCurrentQuoteNo() { return currentQuoteNo; }
    public void setCurrentQuoteNo(String currentQuoteNo) { this.currentQuoteNo = currentQuoteNo; }
    public String getCurrentQuoteStatusName() { return currentQuoteStatusName; }
    public void setCurrentQuoteStatusName(String currentQuoteStatusName) { this.currentQuoteStatusName = currentQuoteStatusName; }
    public String getPriceSource() { return priceSource; }
    public void setPriceSource(String priceSource) { this.priceSource = priceSource; }
    public Long getStayDays() { return stayDays; }
    public void setStayDays(Long stayDays) { this.stayDays = stayDays; }
}
