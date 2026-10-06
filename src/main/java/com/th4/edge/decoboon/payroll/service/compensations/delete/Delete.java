package com.th4.edge.decoboon.payroll.service.compensations.delete;

import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 删除尚未形成薪酬事实的员工薪酬方案。 */
public interface Delete extends Provider<CompensationScheme, CompensationScheme>, Security {
}
