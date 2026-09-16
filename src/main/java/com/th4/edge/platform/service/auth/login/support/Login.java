package com.th4.edge.platform.service.auth.login.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.platform.context.PlatformPasswordContext;
import com.th4.edge.platform.model.PlatformDepartment;
import com.th4.edge.platform.model.PlatformOrg;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.model.PlatformUserStatus;
import com.th4.edge.platform.repository.PlatformUserRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 平台独立登录实现。 */
@Note("平台登录")
@Properties(allow = {
        @AllowProperties(model = PlatformUser.class, properties = {
                "userId", "org", "department", "name", "phone", "role", "status", "lastLoginAt", "createTime"
        }),
        @AllowProperties(model = PlatformOrg.class, properties = {"orgId", "code", "name", "status"}),
        @AllowProperties(model = PlatformDepartment.class, properties = {"deptId", "code", "name", "status"})
})
@Service("platform/auth/login")
@Scope(ScopeTypes.PROTOTYPE)
public class Login implements com.th4.edge.platform.service.auth.login.Login {
    @AutoInject @Length(max = 20) private String phone;
    @AutoInject @Length(max = 64) private String password;
    private final PlatformUserRepository userRepository;
    private final PlatformPasswordContext passwordContext;

    public Login(PlatformUserRepository userRepository, PlatformPasswordContext passwordContext) {
        this.userRepository = userRepository;
        this.passwordContext = passwordContext;
    }

    @Override
    @Transactional
    public PlatformUser call(PlatformUser request) {
        String normalizedPhone = phone == null ? null : phone.trim();
        PlatformUser user = normalizedPhone == null || normalizedPhone.isBlank() ? null : userRepository.findByPhone(normalizedPhone);
        if (user == null || !passwordContext.matches(password, user.getPasswordHash())) {
            throw new DefinedException(new DefaultErrorCode("PLATFORM_AUTH_FAILED", "平台账号或密码错误"));
        }
        if (user.getStatus() != PlatformUserStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("403", "平台账号已停用或锁定"));
        }
        if (user.getOrg() == null || user.getOrg().getStatus() != com.th4.edge.platform.model.PlatformOrgStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("403", "平台组织不可用"));
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return user;
    }
}
