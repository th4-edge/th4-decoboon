package com.th4.edge.decoboon.statistic.service.workbench.quote.page;

import com.th4.edge.decoboon.statistic.model.QuoteWorkbenchItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/**
 * 设计师报价工作台分页服务。
 */
public interface Page extends Provider<Employee, org.springframework.data.domain.Page<QuoteWorkbenchItem>>, Security {
}
