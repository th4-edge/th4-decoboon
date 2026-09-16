package com.th4.edge.tenant.user.access;

import com.th4.edge.tenant.user.configure.DataScopeType;

/**
 * 当前登录员工的不可变数据范围。
 */
public record DataScope(
        DataScopeType type,
        Long comId,
        Long storeId,
        Long teamId,
        Long empId
) {
}
