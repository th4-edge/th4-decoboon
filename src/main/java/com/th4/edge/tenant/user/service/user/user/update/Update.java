package com.th4.edge.tenant.user.service.user.user.update;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
public interface Update extends Provider<Employee, Credentials>, Security {}
