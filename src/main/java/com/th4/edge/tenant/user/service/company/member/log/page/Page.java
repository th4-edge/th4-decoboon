package com.th4.edge.tenant.user.service.company.member.log.page;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.CompanyMemberLog;
import com.th4.edge.tenant.user.model.Employee;
public interface Page extends Provider<Employee, org.springframework.data.domain.Page<CompanyMemberLog>>, Security {}
