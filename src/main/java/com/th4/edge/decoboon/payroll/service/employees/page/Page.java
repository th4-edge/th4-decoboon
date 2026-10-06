package com.th4.edge.decoboon.payroll.service.employees.page;

import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.tenant.user.model.Employee;

public interface Page extends ModelService<Employee, org.springframework.data.domain.Page<Employee>>, Security {
}
