package com.th4.edge.platform.service.tenant.info.support;

import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.integration.tenant.TenantDataAdapter;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformTenant;
import com.th4.edge.platform.model.PlatformUser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 平台企业详情实现。 */
@Note("平台企业详情")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.platform.model.PlatformTenant.class,
                properties = {"comId", "title", "name", "logo", "color", "size", "addr", "createTime", "subId", "planName", "subscriptionStatus", "serviceEndTime", "remainingDays", "usedSeats", "effectiveMaxSeats", "usedStores", "effectiveMaxStores", "enableStore"})
})
@Service("platform/tenant/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.platform.service.tenant.info.Info {
    @AutoInject @NotNull private Long companyId;
    private final PlatformAccessContext accessContext;
    private final TenantDataAdapter tenantDataAdapter;

    public Info(PlatformAccessContext accessContext, TenantDataAdapter tenantDataAdapter) {
        this.accessContext = accessContext;
        this.tenantDataAdapter = tenantDataAdapter;
    }

    @Override
    public PlatformTenant call(@User PlatformUser user) {
        accessContext.requirePermission(user, PlatformPermission.TenantRead);
        return tenantDataAdapter.tenant(companyId);
    }
}
