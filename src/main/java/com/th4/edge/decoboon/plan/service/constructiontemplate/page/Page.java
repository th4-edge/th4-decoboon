package com.th4.edge.decoboon.plan.service.constructiontemplate.page;

import com.th4.edge.decoboon.plan.model.PlanTemplateCatalog;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** construction-template page Provider。 */
public interface Page extends Provider<Employee, PlanTemplateCatalog>, Security {
}
