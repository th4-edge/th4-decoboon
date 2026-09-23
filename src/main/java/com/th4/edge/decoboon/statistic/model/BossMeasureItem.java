package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 企业负责人量房经营明细。 */
@Note("企业负责人量房经营明细")
public class BossMeasureItem {

    private Long measureId;
    private Long customerId;
    private Long propertyId;
    private String customerName;
    private String telNo;
    private String customerStatus;
    private String customerStatusName;
    private String source;
    private String sourceName;
    private Long marketerId;
    private String marketerName;
    private String marketerStoreName;
    private String community;
    private String building;
    private String roomNo;
    private String address;
    private BigDecimal area;
    private String layout;
    private String propertyStatus;
    private String propertyStatusName;
    private Long designerId;
    private String designerName;
    private Long executorId;
    private String executorName;
    private LocalDateTime scheduledAt;
    private LocalDateTime completedAt;
    private String measureStatus;
    private String derivedState;
    private String derivedStateName;
    private String riskLevel;
    private String nextAction;
    private Integer sitePhotoCount;
    private Integer structureImageCount;
    private Integer attachmentCount;
    private Boolean handoffStarted;
    private LocalDateTime handoffStartedAt;
    private Long stayHours;

    public Long getMeasureId() { return measureId; }
    public void setMeasureId(Long measureId) { this.measureId = measureId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    public String getCustomerStatus() { return customerStatus; }
    public void setCustomerStatus(String customerStatus) { this.customerStatus = customerStatus; }
    public String getCustomerStatusName() { return customerStatusName; }
    public void setCustomerStatusName(String customerStatusName) { this.customerStatusName = customerStatusName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public Long getMarketerId() { return marketerId; }
    public void setMarketerId(Long marketerId) { this.marketerId = marketerId; }
    public String getMarketerName() { return marketerName; }
    public void setMarketerName(String marketerName) { this.marketerName = marketerName; }
    public String getMarketerStoreName() { return marketerStoreName; }
    public void setMarketerStoreName(String marketerStoreName) { this.marketerStoreName = marketerStoreName; }
    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public String getLayout() { return layout; }
    public void setLayout(String layout) { this.layout = layout; }
    public String getPropertyStatus() { return propertyStatus; }
    public void setPropertyStatus(String propertyStatus) { this.propertyStatus = propertyStatus; }
    public String getPropertyStatusName() { return propertyStatusName; }
    public void setPropertyStatusName(String propertyStatusName) { this.propertyStatusName = propertyStatusName; }
    public Long getDesignerId() { return designerId; }
    public void setDesignerId(Long designerId) { this.designerId = designerId; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
    public Long getExecutorId() { return executorId; }
    public void setExecutorId(Long executorId) { this.executorId = executorId; }
    public String getExecutorName() { return executorName; }
    public void setExecutorName(String executorName) { this.executorName = executorName; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getMeasureStatus() { return measureStatus; }
    public void setMeasureStatus(String measureStatus) { this.measureStatus = measureStatus; }
    public String getDerivedState() { return derivedState; }
    public void setDerivedState(String derivedState) { this.derivedState = derivedState; }
    public String getDerivedStateName() { return derivedStateName; }
    public void setDerivedStateName(String derivedStateName) { this.derivedStateName = derivedStateName; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public Integer getSitePhotoCount() { return sitePhotoCount; }
    public void setSitePhotoCount(Integer sitePhotoCount) { this.sitePhotoCount = sitePhotoCount; }
    public Integer getStructureImageCount() { return structureImageCount; }
    public void setStructureImageCount(Integer structureImageCount) { this.structureImageCount = structureImageCount; }
    public Integer getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(Integer attachmentCount) { this.attachmentCount = attachmentCount; }
    public Boolean getHandoffStarted() { return handoffStarted; }
    public void setHandoffStarted(Boolean handoffStarted) { this.handoffStarted = handoffStarted; }
    public LocalDateTime getHandoffStartedAt() { return handoffStartedAt; }
    public void setHandoffStartedAt(LocalDateTime handoffStartedAt) { this.handoffStartedAt = handoffStartedAt; }
    public Long getStayHours() { return stayHours; }
    public void setStayHours(Long stayHours) { this.stayHours = stayHours; }
}
