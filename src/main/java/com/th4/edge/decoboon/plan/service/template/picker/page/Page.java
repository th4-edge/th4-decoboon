package com.th4.edge.decoboon.plan.service.template.picker.page;

import com.th4.edge.decoboon.plan.model.PlanTemplateCatalog;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 付款/施工计划选择器分页。 */
public interface Page extends Provider<Employee, PlanTemplateCatalog>, Security {
}
