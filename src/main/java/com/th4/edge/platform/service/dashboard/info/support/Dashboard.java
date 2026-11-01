package com.th4.edge.platform.service.dashboard.info.support;

import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.integration.tenant.TenantDataAdapter;
import com.th4.edge.platform.model.PlatformMetrics;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformUser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 平台运营总览实现。 */
@Note("平台运营总览")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.platform.model.PlatformMetrics.class,
                properties = {"totalCompanies", "activeSubscriptions", "trialSubscriptions", "riskCompanies", "expiringSoon", "newCompaniesThisMonth", "paidAmountLast30Days", "mrr", "seatUsageRate", "storeUsageRate", "subscriptionStatus", "planDistribution", "funnel", "revenueTrend"}),
        @AllowProperties(model = com.th4.edge.platform.model.FunnelMetric.class,
                properties = {"name", "value", "conversionRate"}),
        @AllowProperties(model = com.th4.edge.platform.model.RevenueTrend.class,
                properties = {"month", "orderAmount", "newCompanies", "renewals"})
})
@Service("platform/dashboard/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Dashboard implements com.th4.edge.platform.service.dashboard.info.Dashboard {
    private final PlatformAccessContext accessContext;
    private final TenantDataAdapter tenantDataAdapter;

    public Dashboard(PlatformAccessContext accessContext, TenantDataAdapter tenantDataAdapter) {
        this.accessContext = accessContext;
        this.tenantDataAdapter = tenantDataAdapter;
    }

    @Override
    public PlatformMetrics call(@User PlatformUser user) {
        accessContext.requirePermission(user, PlatformPermission.DashboardRead);
        return tenantDataAdapter.metrics();
    }
}
