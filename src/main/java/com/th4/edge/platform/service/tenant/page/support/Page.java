package com.th4.edge.platform.service.tenant.page.support;

import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.integration.tenant.TenantDataAdapter;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformTenant;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.tenant.billing.configure.SubStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** 平台企业分页实现。 */
@Note("平台企业分页")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.platform.model.PlatformTenant.class,
                properties = {"comId", "title", "name", "logo", "color", "size", "addr", "createTime", "subId", "planName", "subscriptionStatus", "serviceEndTime", "remainingDays", "usedSeats", "effectiveMaxSeats", "usedStores", "effectiveMaxStores", "enableStore"})
})
@Service("platform/tenant/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.platform.service.tenant.page.Page {
    @AutoInject @Length(max = 64) private String keyword;
    @AutoInject private SubStatus status;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final PlatformAccessContext accessContext;
    private final TenantDataAdapter tenantDataAdapter;

    public Page(PlatformAccessContext accessContext, TenantDataAdapter tenantDataAdapter) {
        this.accessContext = accessContext;
        this.tenantDataAdapter = tenantDataAdapter;
    }

    @Override
    public org.springframework.data.domain.Page<PlatformTenant> call(@User PlatformUser user) {
        accessContext.requirePermission(user, PlatformPermission.TenantRead);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return tenantDataAdapter.pageTenants(normalizedKeyword, status, PageRequest.of(page, pageSize));
    }
}
