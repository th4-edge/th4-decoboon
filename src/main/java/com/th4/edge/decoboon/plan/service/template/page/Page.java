package com.th4.edge.decoboon.plan.service.template.page;

import com.th4.edge.decoboon.plan.model.PlanTemplateCatalog;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

public interface Page extends Provider<Employee, PlanTemplateCatalog>, Security {
}
