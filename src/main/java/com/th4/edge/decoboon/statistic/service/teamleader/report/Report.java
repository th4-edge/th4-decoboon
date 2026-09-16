package com.th4.edge.decoboon.statistic.service.teamleader.report;

import com.th4.edge.decoboon.statistic.model.TeamLeaderReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 团队负责人经营报告服务。 */
public interface Report extends Provider<Employee, TeamLeaderReport>, Security {
}
