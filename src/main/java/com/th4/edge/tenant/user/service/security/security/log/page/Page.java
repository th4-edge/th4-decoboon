package com.th4.edge.tenant.user.service.security.security.log.page;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.UserSecurityLog;
public interface Page extends Provider<Employee, org.springframework.data.domain.Page<UserSecurityLog>>, Security {}
