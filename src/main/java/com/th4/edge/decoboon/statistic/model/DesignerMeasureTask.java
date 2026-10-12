package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 设计师待量房任务：显式绑定客户与房屋，并携带服务端主行动和阻断原因。 */
@Note("设计师待量房任务")
public class DesignerMeasureTask {

    @Note("任务稳定 key") private String taskKey;
    @Note("客户编号") private Long customerId;
    @Note("客户姓名") private String customerName;
    @Note("客户手机号") private String telNo;
    @Note("客户状态 key") private String customerStatus;
    @Note("客户状态中文") private String customerStatusName;
    @Note("房屋编号") private Long propertyId;
    @Note("房屋展示名") private String propertyLabel;
    @Note("小区") private String community;
    @Note("楼栋 / 单元") private String building;
    @Note("房号") private String roomNo;
    @Note("详细地址") private String address;
    @Note("建筑面积") private BigDecimal area;
    @Note("户型") private String layout;
    @Note("房屋状态 key") private String propertyStatus;
    @Note("房屋状态中文") private String propertyStatusName;
    @Note("量房记录编号") private Long measureId;
    @Note("量房事实状态") private String measureStatus;
    @Note("预约时间") private LocalDateTime scheduledAt;
    @Note("预约状态 key") private String scheduleStateKey;
    @Note("预约状态中文") private String scheduleStateName;
    @Note("完成时间") private LocalDateTime completedAt;
    @Note("任务状态 key") private String stateKey;
    @Note("任务状态中文") private String stateLabel;
    @Note("风险等级") private String riskLevel = "normal";
    @Note("风险 / 为什么需要处理") private String riskReason;
    @Note("主行动 key") private String mainActionKey;
    @Note("主行动中文") private String mainActionLabel;
    @Note("下一步动作 key") private String nextActionKey;
    @Note("下一步动作中文") private String nextActionLabel;
    @Note("降级并列动作 key（主行动不适用但现场事实已发生时的第二动作）") private String secondaryActionKey;
    @Note("降级并列动作中文") private String secondaryActionLabel;
    @Note("完成后去向说明") private String completionHint;
    @Note("进入完整推进工作区的路由") private String route;
    @Note("是否允许执行主行动") private Boolean allowed = Boolean.TRUE;
    @Note("阻断原因") private String blockReason;
    @Note("现场照片数") private Integer sitePhotoCount = 0;
    @Note("结构图数") private Integer structureImageCount = 0;
    @Note("附件数") private Integer attachmentCount = 0;
    @Note("已记录诉求数") private Integer requirementCount = 0;
    @Note("已建立方案数") private Integer designCount = 0;
    @Note("排序优先级") private Integer priority = 0;

    public String getTaskKey() { return taskKey; }
    public void setTaskKey(String taskKey) { this.taskKey = taskKey; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    public String getCustomerStatus() { return customerStatus; }
    public void setCustomerStatus(String customerStatus) { this.customerStatus = customerStatus; }
    public String getCustomerStatusName() { return customerStatusName; }
    public void setCustomerStatusName(String customerStatusName) { this.customerStatusName = customerStatusName; }
    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
    public String getPropertyLabel() { return propertyLabel; }
    public void setPropertyLabel(String propertyLabel) { this.propertyLabel = propertyLabel; }
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
    public Long getMeasureId() { return measureId; }
    public void setMeasureId(Long measureId) { this.measureId = measureId; }
    public String getMeasureStatus() { return measureStatus; }
    public void setMeasureStatus(String measureStatus) { this.measureStatus = measureStatus; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getScheduleStateKey() { return scheduleStateKey; }
    public void setScheduleStateKey(String scheduleStateKey) { this.scheduleStateKey = scheduleStateKey; }
    public String getScheduleStateName() { return scheduleStateName; }
    public void setScheduleStateName(String scheduleStateName) { this.scheduleStateName = scheduleStateName; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getStateKey() { return stateKey; }
    public void setStateKey(String stateKey) { this.stateKey = stateKey; }
    public String getStateLabel() { return stateLabel; }
    public void setStateLabel(String stateLabel) { this.stateLabel = stateLabel; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }
    public String getMainActionKey() { return mainActionKey; }
    public void setMainActionKey(String mainActionKey) { this.mainActionKey = mainActionKey; }
    public String getMainActionLabel() { return mainActionLabel; }
    public void setMainActionLabel(String mainActionLabel) { this.mainActionLabel = mainActionLabel; }
    public String getNextActionKey() { return nextActionKey; }
    public void setNextActionKey(String nextActionKey) { this.nextActionKey = nextActionKey; }
    public String getNextActionLabel() { return nextActionLabel; }
    public void setNextActionLabel(String nextActionLabel) { this.nextActionLabel = nextActionLabel; }
    public String getSecondaryActionKey() { return secondaryActionKey; }
    public void setSecondaryActionKey(String secondaryActionKey) { this.secondaryActionKey = secondaryActionKey; }
    public String getSecondaryActionLabel() { return secondaryActionLabel; }
    public void setSecondaryActionLabel(String secondaryActionLabel) { this.secondaryActionLabel = secondaryActionLabel; }
    public String getCompletionHint() { return completionHint; }
    public void setCompletionHint(String completionHint) { this.completionHint = completionHint; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public Boolean getAllowed() { return allowed; }
    public void setAllowed(Boolean allowed) { this.allowed = allowed; }
    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
    public Integer getSitePhotoCount() { return sitePhotoCount; }
    public void setSitePhotoCount(Integer sitePhotoCount) { this.sitePhotoCount = sitePhotoCount; }
    public Integer getStructureImageCount() { return structureImageCount; }
    public void setStructureImageCount(Integer structureImageCount) { this.structureImageCount = structureImageCount; }
    public Integer getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(Integer attachmentCount) { this.attachmentCount = attachmentCount; }
    public Integer getRequirementCount() { return requirementCount; }
    public void setRequirementCount(Integer requirementCount) { this.requirementCount = requirementCount; }
    public Integer getDesignCount() { return designCount; }
    public void setDesignCount(Integer designCount) { this.designCount = designCount; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
