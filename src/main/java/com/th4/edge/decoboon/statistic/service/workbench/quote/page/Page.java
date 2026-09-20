package com.th4.edge.decoboon.statistic.service.workbench.quote.page;

import com.th4.edge.decoboon.statistic.model.QuoteWorkbenchItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

import java.util.List;

/**
 * 设计师报价工作台列表服务。
 */
public interface Page extends Provider<Employee, List<QuoteWorkbenchItem>>, Security {
}
