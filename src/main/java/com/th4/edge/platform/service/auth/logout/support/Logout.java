package com.th4.edge.platform.service.auth.logout.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.model.PlatformUser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 平台独立登出实现。 */
@Note("平台登出")
@Service("platform/auth/logout")
@Scope(ScopeTypes.PROTOTYPE)
public class Logout implements com.th4.edge.platform.service.auth.logout.Logout {
    @Override
    public PlatformUser call(@User PlatformUser user) {
        return user;
    }
}
