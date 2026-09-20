package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设计师签约工作台列表项。
 * 聚合"报价已确认、待签约立项"的客户房屋，是销售链路的核心枢纽。
 */
@Note("签约工作台列表项")
public class SignWorkbenchItem {

    private Long custId;
    private Long propertyId;
    private String customerName;
    private String telNo;
    private String community;
    private String addr;
    private BigDecimal area;
    private String customerStatus;
    private String customerStatusName;

    private Long quoteId;
    private String quoteNo;
    private Integer version;
    /** 客户确认的报价金额 */
    private BigDecimal acceptedAmount;
    /** 报价确认时间 */
    private LocalDateTime acceptedAt;
    /** 是否已转项目（已签约） */
    private Boolean converted;
    private Long contractId;
    private String contractStatus;
    private String contractStatusName;
    private java.time.LocalDate signedAt;
    /** 关联项目ID（已签约时有值） */
    private Long projectId;
    private String projectName;

    /** 报价确认后停留天数（核心流失风险指标） */
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
    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public String getQuoteNo() { return quoteNo; }
    public void setQuoteNo(String quoteNo) { this.quoteNo = quoteNo; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public BigDecimal getAcceptedAmount() { return acceptedAmount; }
    public void setAcceptedAmount(BigDecimal acceptedAmount) { this.acceptedAmount = acceptedAmount; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public Boolean getConverted() { return converted; }
    public void setConverted(Boolean converted) { this.converted = converted; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getContractStatus() { return contractStatus; }
    public void setContractStatus(String contractStatus) { this.contractStatus = contractStatus; }
    public String getContractStatusName() { return contractStatusName; }
    public void setContractStatusName(String contractStatusName) { this.contractStatusName = contractStatusName; }
    public java.time.LocalDate getSignedAt() { return signedAt; }
    public void setSignedAt(java.time.LocalDate signedAt) { this.signedAt = signedAt; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public Long getStayDays() { return stayDays; }
    public void setStayDays(Long stayDays) { this.stayDays = stayDays; }
}
