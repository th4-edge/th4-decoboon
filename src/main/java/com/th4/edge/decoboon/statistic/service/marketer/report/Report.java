package com.th4.edge.decoboon.statistic.service.marketer.report;

import com.th4.edge.decoboon.statistic.model.MarketerReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

public interface Report extends Provider<Employee, MarketerReport>, Security {
}
