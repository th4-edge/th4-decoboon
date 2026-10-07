package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDateTime;

/** 角色业务工作台入站队列项：只描述当前对象、下一步动作和进入工作面所需锚点。 */
@Note("角色业务工作台入站队列项")
public class WorkQueueItem {

    @Note("队列项 key") private String queueKey;
    @Note("对象类型") private String objectType;
    @Note("对象编号") private Long objectId;
    @Note("客户编号") private Long customerId;
    @Note("房屋编号") private Long propertyId;
    @Note("项目编号") private Long projectId;
    @Note("标题") private String title;
    @Note("副标题") private String subtitle;
    @Note("状态 key") private String stateKey;
    @Note("状态中文") private String stateLabel;
    @Note("责任人") private String ownerName;
    @Note("下一步动作 key") private String nextActionKey;
    @Note("下一步动作中文") private String nextActionLabel;
    @Note("进入路由") private String route;
    @Note("优先级") private Integer priority = 0;
    @Note("到期/停留时间") private LocalDateTime dueAt;
    @Note("停留天数") private Integer stayDays = 0;
    @Note("风险等级") private String riskLevel = "normal";
    @Note("是否允许执行") private Boolean allowed = Boolean.TRUE;
    @Note("阻断原因") private String blockReason;

    public String getQueueKey() { return queueKey; }
    public void setQueueKey(String queueKey) { this.queueKey = queueKey; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public Long getObjectId() { return objectId; }
    public void setObjectId(Long objectId) { this.objectId = objectId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getStateKey() { return stateKey; }
    public void setStateKey(String stateKey) { this.stateKey = stateKey; }
    public String getStateLabel() { return stateLabel; }
    public void setStateLabel(String stateLabel) { this.stateLabel = stateLabel; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getNextActionKey() { return nextActionKey; }
    public void setNextActionKey(String nextActionKey) { this.nextActionKey = nextActionKey; }
    public String getNextActionLabel() { return nextActionLabel; }
    public void setNextActionLabel(String nextActionLabel) { this.nextActionLabel = nextActionLabel; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public Integer getStayDays() { return stayDays; }
    public void setStayDays(Integer stayDays) { this.stayDays = stayDays; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Boolean getAllowed() { return allowed; }
    public void setAllowed(Boolean allowed) { this.allowed = allowed; }
    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
}
