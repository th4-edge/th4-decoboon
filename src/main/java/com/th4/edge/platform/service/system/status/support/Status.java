package com.th4.edge.platform.service.system.status.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.context.SystemMonitorContext;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.model.SystemStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 平台服务器运行状态实现。 */
@Note("平台服务器运行状态")
@Service("platform/system/status")
@Scope(ScopeTypes.PROTOTYPE)
public class Status implements com.th4.edge.platform.service.system.status.Status {
    private final PlatformAccessContext accessContext;
    private final SystemMonitorContext monitorContext;

    public Status(PlatformAccessContext accessContext, SystemMonitorContext monitorContext) {
        this.accessContext = accessContext;
        this.monitorContext = monitorContext;
    }

    @Override
    public SystemStatus call(@User PlatformUser user) {
        accessContext.requirePermission(user, PlatformPermission.SystemRead);
        return monitorContext.status();
    }
}
