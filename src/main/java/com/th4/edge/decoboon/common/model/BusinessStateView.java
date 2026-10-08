package com.th4.edge.decoboon.common.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDateTime;
import java.util.List;

/** 服务端统一业务状态视图：Web/App 只读消费，不在前端推导状态和下一步。 */
@Note("业务状态视图")
public class BusinessStateView {
    private String objectKey;
    private Long objectId;
    private String status;
    private String statusLabel;
    private String statusTone;
    private List<BusinessDerivedLabel> derivedLabels;
    private BusinessNextAction nextAction;
    private List<String> blockedReasons;
    private String nextOwner;
    private LocalDateTime updatedAt;
    private List<String> businessRefs;

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public Long getObjectId() { return objectId; }
    public void setObjectId(Long objectId) { this.objectId = objectId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
    public String getStatusTone() { return statusTone; }
    public void setStatusTone(String statusTone) { this.statusTone = statusTone; }
    public List<BusinessDerivedLabel> getDerivedLabels() { return derivedLabels; }
    public void setDerivedLabels(List<BusinessDerivedLabel> derivedLabels) { this.derivedLabels = derivedLabels; }
    public BusinessNextAction getNextAction() { return nextAction; }
    public void setNextAction(BusinessNextAction nextAction) { this.nextAction = nextAction; }
    public List<String> getBlockedReasons() { return blockedReasons; }
    public void setBlockedReasons(List<String> blockedReasons) { this.blockedReasons = blockedReasons; }
    public String getNextOwner() { return nextOwner; }
    public void setNextOwner(String nextOwner) { this.nextOwner = nextOwner; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<String> getBusinessRefs() { return businessRefs; }
    public void setBusinessRefs(List<String> businessRefs) { this.businessRefs = businessRefs; }
}
