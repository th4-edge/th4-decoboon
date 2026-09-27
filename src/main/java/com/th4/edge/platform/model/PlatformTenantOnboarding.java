package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 平台企业入驻与负责人服务台账只读投影。 */
@Note("企业入驻服务台账")
@Model
public class PlatformTenantOnboarding {
    @Note("企业ID") private Long comId;
    @Note("企业名称") private String companyTitle;
    @Note("企业简称") private String companyName;
    @Note("企业规模") private String companySize;
    @Note("企业地址") private String companyAddress;
    @Note("企业注册时间") private DateTime companyCreateTime;
    @Note("套餐名称") private String planName;
    @Note("订阅状态") private String subscriptionStatus;
    @Note("剩余天数") private Integer remainingDays;
    @Note("有效负责人数量") private int ownerCount;
    @Note("负责人员工ID") private Long ownerEmpId;
    @Note("负责人姓名") private String ownerName;
    @Note("负责人手机号掩码") private String ownerPhoneMasked;
    @Note("负责人状态") private String ownerStatus;
    @Note("负责人加入时间") private LocalDateTime ownerJoinedAt;
    @Note("账号创建时间") private LocalDateTime accountCreateTime;
    @Note("最近登录时间") private LocalDateTime lastLoginAt;
    @Note("最近登录方式") private String lastLoginType;
    @Note("距最近登录天数") private Long daysSinceLastLogin;
    @Note("登录状态") private String loginStatus;
    @Note("业务阶段") private String businessStage;
    @Note("服务状态") private PlatformOnboardingServiceStatus serviceStatus = PlatformOnboardingServiceStatus.Pending;
    @Note("最近服务时间") private LocalDateTime lastServiceAt;
    @Note("下次跟进时间") private LocalDateTime nextFollowAt;
    @Note("下一步") private String nextAction;
    @Note("负责人列表") private List<PlatformTenantOwner> owners = new ArrayList<>();
    @Note("服务记录") private List<PlatformOnboardingServiceView> serviceRecords = new ArrayList<>();

    public Long getComId() { return comId; }
    public void setComId(Long comId) { this.comId = comId; }
    public String getCompanyTitle() { return companyTitle; }
    public void setCompanyTitle(String companyTitle) { this.companyTitle = companyTitle; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }
    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
    public DateTime getCompanyCreateTime() { return companyCreateTime; }
    public void setCompanyCreateTime(DateTime companyCreateTime) { this.companyCreateTime = companyCreateTime; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public Integer getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Integer remainingDays) { this.remainingDays = remainingDays; }
    public int getOwnerCount() { return ownerCount; }
    public void setOwnerCount(int ownerCount) { this.ownerCount = ownerCount; }
    public Long getOwnerEmpId() { return ownerEmpId; }
    public void setOwnerEmpId(Long ownerEmpId) { this.ownerEmpId = ownerEmpId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerPhoneMasked() { return ownerPhoneMasked; }
    public void setOwnerPhoneMasked(String ownerPhoneMasked) { this.ownerPhoneMasked = ownerPhoneMasked; }
    public String getOwnerStatus() { return ownerStatus; }
    public void setOwnerStatus(String ownerStatus) { this.ownerStatus = ownerStatus; }
    public LocalDateTime getOwnerJoinedAt() { return ownerJoinedAt; }
    public void setOwnerJoinedAt(LocalDateTime ownerJoinedAt) { this.ownerJoinedAt = ownerJoinedAt; }
    public LocalDateTime getAccountCreateTime() { return accountCreateTime; }
    public void setAccountCreateTime(LocalDateTime accountCreateTime) { this.accountCreateTime = accountCreateTime; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public String getLastLoginType() { return lastLoginType; }
    public void setLastLoginType(String lastLoginType) { this.lastLoginType = lastLoginType; }
    public Long getDaysSinceLastLogin() { return daysSinceLastLogin; }
    public void setDaysSinceLastLogin(Long daysSinceLastLogin) { this.daysSinceLastLogin = daysSinceLastLogin; }
    public String getLoginStatus() { return loginStatus; }
    public void setLoginStatus(String loginStatus) { this.loginStatus = loginStatus; }
    public String getBusinessStage() { return businessStage; }
    public void setBusinessStage(String businessStage) { this.businessStage = businessStage; }
    public PlatformOnboardingServiceStatus getServiceStatus() { return serviceStatus; }
    public void setServiceStatus(PlatformOnboardingServiceStatus serviceStatus) { this.serviceStatus = serviceStatus; }
    public LocalDateTime getLastServiceAt() { return lastServiceAt; }
    public void setLastServiceAt(LocalDateTime lastServiceAt) { this.lastServiceAt = lastServiceAt; }
    public LocalDateTime getNextFollowAt() { return nextFollowAt; }
    public void setNextFollowAt(LocalDateTime nextFollowAt) { this.nextFollowAt = nextFollowAt; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public List<PlatformTenantOwner> getOwners() { return owners; }
    public void setOwners(List<PlatformTenantOwner> owners) { this.owners = owners; }
    public List<PlatformOnboardingServiceView> getServiceRecords() { return serviceRecords; }
    public void setServiceRecords(List<PlatformOnboardingServiceView> serviceRecords) { this.serviceRecords = serviceRecords; }
}
