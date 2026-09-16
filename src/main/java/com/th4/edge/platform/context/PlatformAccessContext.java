package com.th4.edge.platform.context;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.platform.model.PlatformOrgStatus;
import com.th4.edge.platform.model.PlatformPermission;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.model.PlatformUserStatus;
import com.th4.edge.platform.repository.PlatformUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 平台独立访问控制上下文。 */
@Note("平台访问控制")
@Component
public class PlatformAccessContext {
    private final PlatformUserRepository userRepository;

    public PlatformAccessContext(PlatformUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PlatformUser requireUser(PlatformUser principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new DefinedException(new DefaultErrorCode("401", "平台登录状态已失效"));
        }
        PlatformUser user = userRepository.findWithOrgAndDepartmentByUserId(principal.getUserId());
        if (user == null) {
            throw new DefinedException(new DefaultErrorCode("401", "平台用户不存在"));
        }
        if (user.getStatus() != PlatformUserStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("403", "平台用户已停用或锁定"));
        }
        if (user.getOrg() == null || user.getOrg().getStatus() != PlatformOrgStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("403", "平台组织不可用"));
        }
        return user;
    }

    @Transactional(readOnly = true)
    public PlatformUser requirePermission(PlatformUser principal, PlatformPermission permission) {
        PlatformUser user = requireUser(principal);
        if (!user.getRole().getPermissions().contains(permission)) {
            throw new DefinedException(new DefaultErrorCode("403", "当前岗位无权执行该操作"));
        }
        return user;
    }
}
