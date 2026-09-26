package com.th4.edge.decoboon.ai.billing.service.account.report;

import com.th4.edge.decoboon.ai.billing.model.AiAccountReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

public interface Report extends Provider<Employee, AiAccountReport>, Security {
}
