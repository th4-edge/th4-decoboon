package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;

import java.time.LocalDateTime;

/** 平台企业负责人只读摘要。 */
@Note("平台企业负责人摘要")
@Model
public class PlatformTenantOwner {
    @Note("员工ID") private Long empId;
    @Note("姓名") private String name;
    @Note("手机号掩码") private String phoneMasked;
    @Note("员工状态") private String status;
    @Note("加入企业时间") private LocalDateTime joinedAt;
    @Note("账号创建时间") private LocalDateTime accountCreateTime;
    @Note("最近登录时间") private LocalDateTime lastLoginAt;
    @Note("最近登录方式") private String lastLoginType;

    public Long getEmpId() { return empId; }
    public void setEmpId(Long empId) { this.empId = empId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhoneMasked() { return phoneMasked; }
    public void setPhoneMasked(String phoneMasked) { this.phoneMasked = phoneMasked; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getAccountCreateTime() { return accountCreateTime; }
    public void setAccountCreateTime(LocalDateTime accountCreateTime) { this.accountCreateTime = accountCreateTime; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public String getLastLoginType() { return lastLoginType; }
    public void setLastLoginType(String lastLoginType) { this.lastLoginType = lastLoginType; }
}
