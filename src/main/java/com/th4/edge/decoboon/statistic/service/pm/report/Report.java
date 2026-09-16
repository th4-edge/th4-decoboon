package com.th4.edge.decoboon.statistic.service.pm.report;

import com.th4.edge.decoboon.statistic.model.PmReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 项目经理履约与业绩报告服务。 */
public interface Report extends Provider<Employee, PmReport>, Security {
}
