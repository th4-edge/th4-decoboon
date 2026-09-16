package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

import java.util.EnumSet;
import java.util.Set;

/** 平台组织岗位。 */
@Note("平台组织岗位")
public enum PlatformRole implements Enumerable<String> {
    SuperAdmin("平台超级管理员", EnumSet.allOf(PlatformPermission.class)),
    OperationsManager("平台运营主管", EnumSet.of(
            PlatformPermission.DashboardRead, PlatformPermission.TenantRead,
            PlatformPermission.TenantSensitiveRead, PlatformPermission.SubscriptionRead,
            PlatformPermission.SubscriptionWrite, PlatformPermission.PlanRead,
            PlatformPermission.AuditRead, PlatformPermission.SystemRead)),
    CustomerSuccess("客户成功", EnumSet.of(
            PlatformPermission.DashboardRead, PlatformPermission.TenantRead,
            PlatformPermission.SubscriptionRead, PlatformPermission.SubscriptionWrite,
            PlatformPermission.AuditSelfRead)),
    BillingOperator("财务运营", EnumSet.of(
            PlatformPermission.DashboardRead, PlatformPermission.TenantRead,
            PlatformPermission.SubscriptionRead, PlatformPermission.SubscriptionWrite,
            PlatformPermission.AuditRead)),
    GrowthOperator("市场增长", EnumSet.of(
            PlatformPermission.DashboardRead, PlatformPermission.TenantRead,
            PlatformPermission.SubscriptionRead, PlatformPermission.PlanRead)),
    SreOperator("平台可靠性", EnumSet.of(
            PlatformPermission.DashboardRead, PlatformPermission.TenantRead,
            PlatformPermission.SubscriptionRead, PlatformPermission.SystemRead,
            PlatformPermission.AuditRead)),
    Auditor("审计合规", EnumSet.of(
            PlatformPermission.DashboardRead, PlatformPermission.TenantRead,
            PlatformPermission.SubscriptionRead, PlatformPermission.AuditRead,
            PlatformPermission.SystemRead));

    private final String name;
    private final Set<PlatformPermission> permissions;

    PlatformRole(String name, Set<PlatformPermission> permissions) {
        this.name = name;
        this.permissions = permissions;
    }

    @Override public String getValue() { return name(); }
    @Override public String getName() { return name; }
    public Set<PlatformPermission> getPermissions() { return permissions; }
}
