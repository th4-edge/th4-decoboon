package com.th4.edge.platform.service.plan.list.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.integration.tenant.TenantDataAdapter;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformPlan;
import com.th4.edge.platform.model.PlatformUser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 平台可售套餐列表实现。 */
@Note("平台可售套餐列表")
@Service("platform/plan/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.platform.service.plan.list.List {
    private final PlatformAccessContext accessContext;
    private final TenantDataAdapter tenantDataAdapter;

    public List(PlatformAccessContext accessContext, TenantDataAdapter tenantDataAdapter) {
        this.accessContext = accessContext;
        this.tenantDataAdapter = tenantDataAdapter;
    }

    @Override
    public java.util.List<PlatformPlan> call(@User PlatformUser user) {
        accessContext.requirePermission(user, PlatformPermission.PlanRead);
        return tenantDataAdapter.plans();
    }
}
