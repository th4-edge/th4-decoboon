package com.th4.edge.tenant.user.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EntryInviteType;
import com.th4.edge.tenant.user.configure.InviteStatus;

import java.time.LocalDateTime;

/** 当前已验证手机号可处理的待接受邀请投影。 */
@Note("登录后待处理邀请")
public class PendingEntryInvite {

    @Note("邀请类型") private EntryInviteType type;
    @Note("邀请索引") private Long inviteId;
    @Note("企业名称") private String companyName;
    @Note("企业Logo") private String companyLogo;
    @Note("企业品牌色") private String companyColor;
    @Note("邀请人") private String inviterName;
    @Note("员工岗位") private EmployeeRole role;
    @Note("门店") private String storeName;
    @Note("项目名称") private String projectName;
    @Note("房屋地址") private String propertyAddress;
    @Note("业主姓名") private String customerName;
    @Note("脱敏手机号") private String maskedPhone;
    @Note("邀请状态") private InviteStatus status;
    @Note("过期时间") private LocalDateTime expireTime;

    public EntryInviteType getType() { return type; }
    public void setType(EntryInviteType type) { this.type = type; }
    public Long getInviteId() { return inviteId; }
    public void setInviteId(Long inviteId) { this.inviteId = inviteId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyLogo() { return companyLogo; }
    public void setCompanyLogo(String companyLogo) { this.companyLogo = companyLogo; }
    public String getCompanyColor() { return companyColor; }
    public void setCompanyColor(String companyColor) { this.companyColor = companyColor; }
    public String getInviterName() { return inviterName; }
    public void setInviterName(String inviterName) { this.inviterName = inviterName; }
    public EmployeeRole getRole() { return role; }
    public void setRole(EmployeeRole role) { this.role = role; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getMaskedPhone() { return maskedPhone; }
    public void setMaskedPhone(String maskedPhone) { this.maskedPhone = maskedPhone; }
    public InviteStatus getStatus() { return status; }
    public void setStatus(InviteStatus status) { this.status = status; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
}
