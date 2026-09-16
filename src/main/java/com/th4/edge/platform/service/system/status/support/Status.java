package com.th4.edge.platform.service.system.status.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.context.PlatformAdminContext;
import com.th4.edge.platform.context.SystemMonitorContext;
import com.th4.edge.platform.model.SystemStatus;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 平台服务器运行状态实现。 */
@Note("平台服务器运行状态")
@Service("platform/system/status")
@Scope(ScopeTypes.PROTOTYPE)
public class Status implements com.th4.edge.platform.service.system.status.Status {

    @User
    private Employee employee;

    private final PlatformAdminContext platformContext;
    private final SystemMonitorContext monitorContext;

    public Status(PlatformAdminContext platformContext, SystemMonitorContext monitorContext) {
        this.platformContext = platformContext;
        this.monitorContext = monitorContext;
    }

    @Override
    public SystemStatus call(SystemStatus request) {
        platformContext.requireAdmin(employee);
        return monitorContext.status();
    }
}
