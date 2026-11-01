package com.th4.edge.platform.service.subscription.update.support;


import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Gt;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.integration.tenant.TenantDataAdapter;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformSubscription;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.tenant.billing.configure.SubStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 平台人工调整订阅实现。 */
@Note("平台人工调整订阅")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.platform.model.PlatformSubscription.class,
                properties = {"subId", "comId", "companyTitle", "planCode", "planName", "status", "serviceEndTime", "remainingDays", "extraSeats", "extraStores", "usedSeats", "effectiveMaxSeats", "usedStores", "effectiveMaxStores", "writable", "expired"})
})
@Service("platform/subscription/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.platform.service.subscription.update.Update {
    @AutoInject @NotNull private Long subId;
    @AutoInject @Gt("0") private Integer days;
    @AutoInject @Length(max = 32) private String planCode;
    @AutoInject private SubStatus status;
    @AutoInject @Gt("-1") private Integer extraSeats;
    @AutoInject @Gt("-1") private Integer extraStores;
    @AutoInject @NotEmpty @Length(max = 500) private String reason;
    private final PlatformAccessContext accessContext;
    private final TenantDataAdapter tenantDataAdapter;

    public Update(PlatformAccessContext accessContext, TenantDataAdapter tenantDataAdapter) {
        this.accessContext = accessContext;
        this.tenantDataAdapter = tenantDataAdapter;
    }

    @Override
    public PlatformSubscription call(@User PlatformUser user) {
        PlatformUser operator = accessContext.requirePermission(user, PlatformPermission.SubscriptionWrite);
        return tenantDataAdapter.updateSubscription(operator, subId, days, planCode, status,
                extraSeats, extraStores, reason);
    }
}
