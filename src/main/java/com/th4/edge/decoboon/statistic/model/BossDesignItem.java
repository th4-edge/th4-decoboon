package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 企业负责人视角的客户房屋设计经营事实。 */
@Note("企业负责人设计经营明细")
public class BossDesignItem {

    private Long custId;
    private Long propertyId;
    private String customerName;
    private String telNo;
    private String community;
    private String addr;
    private BigDecimal area;

    private Long designerId;
    private String designerName;
    private String designerStoreName;
    private LocalDateTime measureAt;
    private LocalDateTime pendingMeasureAt;

    private String stage;
    private String stageName;
    private String nextAction;
    private Long stayDays;
    private String riskLevel;

    private Integer requirementCount;
    private String latestRequirementTitle;

    private Integer schemeCount;
    private Long latestSchemeId;
    private String latestSchemeNo;
    private String latestSchemeName;
    private String latestSchemeStatus;
    private String latestSchemeStatusName;
    private Integer latestSchemeSpaceCount;
    private Integer latestSchemeItemCount;
    private LocalDateTime latestDesignAt;

    private Integer quoteCount;
    private Long latestQuoteId;
    private String latestQuoteNo;
    private Integer latestQuoteVersion;
    private BigDecimal latestQuoteAmount;
    private String latestQuoteStatus;
    private String latestQuoteStatusName;
    private LocalDateTime latestQuoteSubmittedAt;
    private LocalDateTime latestQuoteAcceptedAt;

    private Integer talkCount;
    private Long latestTalkId;
    private String latestTalkNo;
    private String latestTalkStatus;
    private String latestTalkStatusName;
    private LocalDateTime latestTalkBuiltAt;
    private String latestTalkOutputUrl;

    private Boolean customerConfirmed;
    private LocalDateTime customerConfirmedAt;

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
    public Long getDesignerId() { return designerId; }
    public void setDesignerId(Long designerId) { this.designerId = designerId; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
    public String getDesignerStoreName() { return designerStoreName; }
    public void setDesignerStoreName(String designerStoreName) { this.designerStoreName = designerStoreName; }
    public LocalDateTime getMeasureAt() { return measureAt; }
    public void setMeasureAt(LocalDateTime measureAt) { this.measureAt = measureAt; }
    public LocalDateTime getPendingMeasureAt() { return pendingMeasureAt; }
    public void setPendingMeasureAt(LocalDateTime pendingMeasureAt) { this.pendingMeasureAt = pendingMeasureAt; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public Long getStayDays() { return stayDays; }
    public void setStayDays(Long stayDays) { this.stayDays = stayDays; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Integer getRequirementCount() { return requirementCount; }
    public void setRequirementCount(Integer requirementCount) { this.requirementCount = requirementCount; }
    public String getLatestRequirementTitle() { return latestRequirementTitle; }
    public void setLatestRequirementTitle(String latestRequirementTitle) { this.latestRequirementTitle = latestRequirementTitle; }
    public Integer getSchemeCount() { return schemeCount; }
    public void setSchemeCount(Integer schemeCount) { this.schemeCount = schemeCount; }
    public Long getLatestSchemeId() { return latestSchemeId; }
    public void setLatestSchemeId(Long latestSchemeId) { this.latestSchemeId = latestSchemeId; }
    public String getLatestSchemeNo() { return latestSchemeNo; }
    public void setLatestSchemeNo(String latestSchemeNo) { this.latestSchemeNo = latestSchemeNo; }
    public String getLatestSchemeName() { return latestSchemeName; }
    public void setLatestSchemeName(String latestSchemeName) { this.latestSchemeName = latestSchemeName; }
    public String getLatestSchemeStatus() { return latestSchemeStatus; }
    public void setLatestSchemeStatus(String latestSchemeStatus) { this.latestSchemeStatus = latestSchemeStatus; }
    public String getLatestSchemeStatusName() { return latestSchemeStatusName; }
    public void setLatestSchemeStatusName(String latestSchemeStatusName) { this.latestSchemeStatusName = latestSchemeStatusName; }
    public Integer getLatestSchemeSpaceCount() { return latestSchemeSpaceCount; }
    public void setLatestSchemeSpaceCount(Integer latestSchemeSpaceCount) { this.latestSchemeSpaceCount = latestSchemeSpaceCount; }
    public Integer getLatestSchemeItemCount() { return latestSchemeItemCount; }
    public void setLatestSchemeItemCount(Integer latestSchemeItemCount) { this.latestSchemeItemCount = latestSchemeItemCount; }
    public LocalDateTime getLatestDesignAt() { return latestDesignAt; }
    public void setLatestDesignAt(LocalDateTime latestDesignAt) { this.latestDesignAt = latestDesignAt; }
    public Integer getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Integer quoteCount) { this.quoteCount = quoteCount; }
    public Long getLatestQuoteId() { return latestQuoteId; }
    public void setLatestQuoteId(Long latestQuoteId) { this.latestQuoteId = latestQuoteId; }
    public String getLatestQuoteNo() { return latestQuoteNo; }
    public void setLatestQuoteNo(String latestQuoteNo) { this.latestQuoteNo = latestQuoteNo; }
    public Integer getLatestQuoteVersion() { return latestQuoteVersion; }
    public void setLatestQuoteVersion(Integer latestQuoteVersion) { this.latestQuoteVersion = latestQuoteVersion; }
    public BigDecimal getLatestQuoteAmount() { return latestQuoteAmount; }
    public void setLatestQuoteAmount(BigDecimal latestQuoteAmount) { this.latestQuoteAmount = latestQuoteAmount; }
    public String getLatestQuoteStatus() { return latestQuoteStatus; }
    public void setLatestQuoteStatus(String latestQuoteStatus) { this.latestQuoteStatus = latestQuoteStatus; }
    public String getLatestQuoteStatusName() { return latestQuoteStatusName; }
    public void setLatestQuoteStatusName(String latestQuoteStatusName) { this.latestQuoteStatusName = latestQuoteStatusName; }
    public LocalDateTime getLatestQuoteSubmittedAt() { return latestQuoteSubmittedAt; }
    public void setLatestQuoteSubmittedAt(LocalDateTime latestQuoteSubmittedAt) { this.latestQuoteSubmittedAt = latestQuoteSubmittedAt; }
    public LocalDateTime getLatestQuoteAcceptedAt() { return latestQuoteAcceptedAt; }
    public void setLatestQuoteAcceptedAt(LocalDateTime latestQuoteAcceptedAt) { this.latestQuoteAcceptedAt = latestQuoteAcceptedAt; }
    public Integer getTalkCount() { return talkCount; }
    public void setTalkCount(Integer talkCount) { this.talkCount = talkCount; }
    public Long getLatestTalkId() { return latestTalkId; }
    public void setLatestTalkId(Long latestTalkId) { this.latestTalkId = latestTalkId; }
    public String getLatestTalkNo() { return latestTalkNo; }
    public void setLatestTalkNo(String latestTalkNo) { this.latestTalkNo = latestTalkNo; }
    public String getLatestTalkStatus() { return latestTalkStatus; }
    public void setLatestTalkStatus(String latestTalkStatus) { this.latestTalkStatus = latestTalkStatus; }
    public String getLatestTalkStatusName() { return latestTalkStatusName; }
    public void setLatestTalkStatusName(String latestTalkStatusName) { this.latestTalkStatusName = latestTalkStatusName; }
    public LocalDateTime getLatestTalkBuiltAt() { return latestTalkBuiltAt; }
    public void setLatestTalkBuiltAt(LocalDateTime latestTalkBuiltAt) { this.latestTalkBuiltAt = latestTalkBuiltAt; }
    public String getLatestTalkOutputUrl() { return latestTalkOutputUrl; }
    public void setLatestTalkOutputUrl(String latestTalkOutputUrl) { this.latestTalkOutputUrl = latestTalkOutputUrl; }
    public Boolean getCustomerConfirmed() { return customerConfirmed; }
    public void setCustomerConfirmed(Boolean customerConfirmed) { this.customerConfirmed = customerConfirmed; }
    public LocalDateTime getCustomerConfirmedAt() { return customerConfirmedAt; }
    public void setCustomerConfirmedAt(LocalDateTime customerConfirmedAt) { this.customerConfirmedAt = customerConfirmedAt; }
}
