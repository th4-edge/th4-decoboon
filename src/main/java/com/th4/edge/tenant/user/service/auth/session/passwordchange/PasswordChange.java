package com.th4.edge.tenant.user.service.auth.session.passwordchange;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;

/** 登录员工设置或修改 Web 登录密码。 */
public interface PasswordChange extends Provider<Employee, Credentials>, Security {}
