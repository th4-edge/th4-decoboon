package com.th4.edge.tenant.user.service.security.security.limit.info.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.CompanyJoinLimit;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.CompanyJoinLimitRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
@Note("加入限制信息")
@Properties(allow = {
        @AllowProperties(model = CompanyJoinLimit.class, properties = {"limitId", "user", "phone", "lastJoinAt", "lastResignAt", "joinCount180d", "lastInviteAcceptAt", "blacklistUntil"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Credentials.class,
                properties = {"creId", "openId", "unionId", "phone", "status", "employee"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Employee.class, properties = {"empId", "name", "role", "status"})
})
@Service("security/security/limit/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.tenant.user.service.security.security.limit.info.Info {
    @User private Employee employee;
    private final TenantContext tenantContext;
    private final CompanyJoinLimitRepository limitRepository;
    private final UserIdentityContext userContext;
    public Info(TenantContext tenantContext, CompanyJoinLimitRepository limitRepository, UserIdentityContext userContext) { this.tenantContext = tenantContext; this.limitRepository = limitRepository; this.userContext = userContext; }
    @Override
    public CompanyJoinLimit call(Employee request) {
        Employee current = tenantContext.requireEmployee(employee);
        com.th4.edge.tenant.user.model.Credentials credential = userContext.requireWx(current);
        if (credential == null) return null;
        return limitRepository.findByUser_CreId(credential.getCreId()).orElse(null);
    }
}
