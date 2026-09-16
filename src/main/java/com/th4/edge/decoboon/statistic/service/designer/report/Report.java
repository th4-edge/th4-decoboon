package com.th4.edge.decoboon.statistic.service.designer.report;

import com.th4.edge.decoboon.statistic.model.DesignerReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 设计师经营报告服务。 */
public interface Report extends Provider<Employee, DesignerReport>, Security {
}
