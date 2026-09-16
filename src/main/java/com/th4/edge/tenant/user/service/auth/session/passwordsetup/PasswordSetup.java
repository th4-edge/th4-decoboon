package com.th4.edge.tenant.user.service.auth.session.passwordsetup;

import com.th4.edge.tenant.user.model.Credentials;

/** 受 bootstrap token 保护的公开首次设密入口。 */
public interface PasswordSetup extends com.th4.edge.framework.provider.security.login.Login<Credentials> {}
