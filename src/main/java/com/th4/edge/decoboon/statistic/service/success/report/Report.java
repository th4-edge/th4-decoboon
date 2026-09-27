package com.th4.edge.decoboon.statistic.service.success.report;

import com.th4.edge.decoboon.statistic.model.SuccessProgramReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 企业负责人用户成功与流失风险看板。 */
public interface Report extends Provider<Employee, SuccessProgramReport>, Security {
}
