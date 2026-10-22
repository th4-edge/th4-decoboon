package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;

import java.util.EnumSet;
import java.util.Set;

/**
 * 员工角色应用边界。
 *
 * <p>DEC-082：TeamLeader 仅保留历史枚举和历史关系兼容，不再是可分配、可登录或可使用的
 * 目标角色。服务端必须统一收口，不能依赖 App/Web 隐藏入口。</p>
 */
public final class EmployeeRolePolicy {

    public static final String FROZEN_LOGIN_MESSAGE =
            "团队负责人角色已冻结，请联系企业负责人改为其他岗位";
    public static final String FROZEN_ASSIGN_MESSAGE =
            "团队负责人角色已冻结，不能用于新邀请或岗位调整";
    public static final String FROZEN_INVITE_MESSAGE =
            "团队负责人角色已冻结，该邀请不可接受";

    private static final Set<EmployeeRole> TARGET_ROLES = EnumSet.of(
            EmployeeRole.Boss,
            EmployeeRole.StoreManager,
            EmployeeRole.Marketer,
            EmployeeRole.Designer,
            EmployeeRole.Pm,
            EmployeeRole.Finance
    );

    private EmployeeRolePolicy() {
    }

    public static boolean isApplicationRole(EmployeeRole role) {
        return role != null && TARGET_ROLES.contains(role);
    }

    public static void requireApplicationAccess(Employee employee) {
        if (employee != null && !isApplicationRole(employee.getRole())) {
            throw new DefinedException(new DefaultErrorCode("403", FROZEN_LOGIN_MESSAGE));
        }
    }

    public static void requireAssignableRole(EmployeeRole role) {
        if (!isApplicationRole(role)) {
            throw new DefinedException(new DefaultErrorCode("403", FROZEN_ASSIGN_MESSAGE));
        }
    }

    public static void requireInviteAcceptable(EmployeeRole role) {
        if (!isApplicationRole(role)) {
            throw new DefinedException(new DefaultErrorCode("403", FROZEN_INVITE_MESSAGE));
        }
    }
}
