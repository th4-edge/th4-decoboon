package com.th4.edge.platform.service.auth.info.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.model.PlatformDepartment;
import com.th4.edge.platform.model.PlatformOrg;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformUser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 平台当前用户实现。 */
@Note("平台当前用户")
@Properties(allow = {
        @AllowProperties(model = PlatformUser.class, properties = {
                "userId", "org", "department", "name", "phone", "role", "status", "lastLoginAt", "createTime", "permissions"
        }),
        @AllowProperties(model = PlatformOrg.class, properties = {"orgId", "code", "name", "status"}),
        @AllowProperties(model = PlatformDepartment.class, properties = {"deptId", "code", "name", "status"})
})
@Service("platform/auth/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.platform.service.auth.info.Info {
    private final PlatformAccessContext accessContext;
    public Info(PlatformAccessContext accessContext) { this.accessContext = accessContext; }

    @Override
    public PlatformUser call(@User PlatformUser user) {
        PlatformUser current = accessContext.requireUser(user);
        current.setPermissions(current.getRole().getPermissions());
        return current;
    }
}
