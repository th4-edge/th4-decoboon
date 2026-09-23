package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 企业负责人视角的客户房屋报价谈单事实。 */
@Note("企业负责人报价谈单房屋")
public class BossQuoteProperty {

    private Long propertyId;
    private String propertyNo;
    private String community;
    private String addr;
    private BigDecimal area;
    private String designerName;

    private Integer quoteCount;
    private Long latestQuoteId;
    private String latestQuoteNo;
    private Integer latestQuoteVersion;
    private BigDecimal latestQuoteAmount;
    private String latestQuoteStatus;
    private String latestQuoteStatusName;
    private LocalDateTime latestQuoteCreatedAt;
    private LocalDateTime latestQuoteSubmittedAt;
    private LocalDateTime latestQuoteAcceptedAt;

    private Integer talkCount;
    private Long latestTalkId;
    private String latestTalkStatus;
    private String latestTalkStatusName;
    private LocalDateTime latestTalkBuiltAt;
    private String latestTalkOutputUrl;

    private Boolean customerConfirmed;
    private LocalDateTime customerConfirmedAt;
    private String nextAction;
    private Long stayDays;
    private String riskLevel;
    private List<BossQuoteHistory> history = List.of();

    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
    public String getPropertyNo() { return propertyNo; }
    public void setPropertyNo(String propertyNo) { this.propertyNo = propertyNo; }
    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
    public String getAddr() { return addr; }
    public void setAddr(String addr) { this.addr = addr; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
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
    public LocalDateTime getLatestQuoteCreatedAt() { return latestQuoteCreatedAt; }
    public void setLatestQuoteCreatedAt(LocalDateTime latestQuoteCreatedAt) { this.latestQuoteCreatedAt = latestQuoteCreatedAt; }
    public LocalDateTime getLatestQuoteSubmittedAt() { return latestQuoteSubmittedAt; }
    public void setLatestQuoteSubmittedAt(LocalDateTime latestQuoteSubmittedAt) { this.latestQuoteSubmittedAt = latestQuoteSubmittedAt; }
    public LocalDateTime getLatestQuoteAcceptedAt() { return latestQuoteAcceptedAt; }
    public void setLatestQuoteAcceptedAt(LocalDateTime latestQuoteAcceptedAt) { this.latestQuoteAcceptedAt = latestQuoteAcceptedAt; }
    public Integer getTalkCount() { return talkCount; }
    public void setTalkCount(Integer talkCount) { this.talkCount = talkCount; }
    public Long getLatestTalkId() { return latestTalkId; }
    public void setLatestTalkId(Long latestTalkId) { this.latestTalkId = latestTalkId; }
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
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public Long getStayDays() { return stayDays; }
    public void setStayDays(Long stayDays) { this.stayDays = stayDays; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<BossQuoteHistory> getHistory() { return history; }
    public void setHistory(List<BossQuoteHistory> history) { this.history = history; }
}
