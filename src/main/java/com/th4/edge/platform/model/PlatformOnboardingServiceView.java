package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;

import java.time.LocalDateTime;

/** 企业入驻服务记录只读视图。 */
@Note("企业入驻服务记录")
@Model
public class PlatformOnboardingServiceView {
    @Note("服务记录ID") private Long serviceId;
    @Note("服务状态") private PlatformOnboardingServiceStatus status;
    @Note("沟通结果") private String contactResult;
    @Note("下一步") private String nextAction;
    @Note("下次跟进时间") private LocalDateTime nextFollowAt;
    @Note("备注") private String remark;
    @Note("操作人") private String operatorName;
    @Note("记录时间") private LocalDateTime createTime;

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public PlatformOnboardingServiceStatus getStatus() { return status; }
    public void setStatus(PlatformOnboardingServiceStatus status) { this.status = status; }
    public String getContactResult() { return contactResult; }
    public void setContactResult(String contactResult) { this.contactResult = contactResult; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public LocalDateTime getNextFollowAt() { return nextFollowAt; }
    public void setNextFollowAt(LocalDateTime nextFollowAt) { this.nextFollowAt = nextFollowAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
