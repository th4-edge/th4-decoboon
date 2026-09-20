package com.th4.edge.decoboon.statistic.service.workbench.design.page;

import com.th4.edge.decoboon.statistic.model.DesignWorkbenchItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

import java.util.List;

/**
 * 设计师设计工作台列表服务。
 */
public interface Page extends Provider<Employee, List<DesignWorkbenchItem>>, Security {
}
