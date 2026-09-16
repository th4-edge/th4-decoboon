package com.th4.edge.tenant.user.service.user.employee.page;

import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/**
 * 当前企业员工分页服务。
 */
public interface Page extends ModelService<Employee, org.springframework.data.domain.Page<Employee>>, Security {
}
