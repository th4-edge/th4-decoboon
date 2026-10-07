package com.th4.edge.decoboon.statistic.service.workbench.designer.report;

import com.th4.edge.decoboon.statistic.model.DesignerWorkbenchReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 设计师业务工作台聚合服务。 */
public interface Report extends Provider<Employee, DesignerWorkbenchReport>, Security {
}
