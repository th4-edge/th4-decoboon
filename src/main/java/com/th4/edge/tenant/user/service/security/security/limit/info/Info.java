package com.th4.edge.tenant.user.service.security.security.limit.info;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.CompanyJoinLimit;
import com.th4.edge.tenant.user.model.Employee;
public interface Info extends Provider<Employee, CompanyJoinLimit>, Security {}
