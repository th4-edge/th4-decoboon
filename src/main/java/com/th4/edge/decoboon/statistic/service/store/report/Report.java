package com.th4.edge.decoboon.statistic.service.store.report;

import com.th4.edge.decoboon.statistic.model.StoreManagerReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 门店店长经营报告服务。 */
public interface Report extends Provider<Employee, StoreManagerReport>, Security {
}
