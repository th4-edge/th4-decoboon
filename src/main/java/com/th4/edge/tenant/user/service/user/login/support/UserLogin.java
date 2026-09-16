package com.th4.edge.tenant.user.service.user.login.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.security.Token;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Note("员工登陆")
@Service("user/login")
@Scope(ScopeTypes.PROTOTYPE)
public class Login implements com.th4.edge.tenant.user.service.user.login.Login {
    @Override
    public Token call(Employee employee) {
        return null;
    }
}
