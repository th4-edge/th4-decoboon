package com.th4.edge.tenant.user.service.auth.session.passwordlogout;

import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 登录员工退出。 */
public interface PasswordLogout extends com.th4.edge.framework.provider.security.logout.Logout<Employee>, Security {}
