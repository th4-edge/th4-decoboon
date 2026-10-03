package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 平台运营能力。 */
@Note("平台运营能力")
public enum PlatformPermission implements Enumerable<String> {
    DashboardRead("运营总览读取"),
    TenantRead("企业脱敏读取"),
    TenantSensitiveRead("企业敏感字段读取"),
    SubscriptionRead("订阅读取"),
    SubscriptionWrite("订阅调整"),
    SubscriptionHighRiskWrite("高风险订阅调整"),
    PlanRead("套餐读取"),
    PlanWrite("套餐维护"),
    AuditRead("审计读取"),
    AuditSelfRead("本人审计读取"),
    SystemRead("系统状态读取"),
    UserManage("平台用户管理"),
    TenantOnboardingRead("企业入驻台账读取"),
    TenantOnboardingSensitiveRead("企业入驻敏感字段读取"),
    TenantOwnerPhoneReveal("企业负责人手机号揭示"),
    TenantOnboardingServiceWrite("企业入驻服务写入");

    private final String name;
    PlatformPermission(String name) { this.name = name; }
    @Override public String getValue() { return name(); }
    @Override public String getName() { return name; }
}
