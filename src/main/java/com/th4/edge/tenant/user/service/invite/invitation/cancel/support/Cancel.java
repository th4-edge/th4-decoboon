package com.th4.edge.tenant.user.service.invite.invitation.cancel.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("取消邀请")
@Properties(allow = {
        @AllowProperties(model = Invite.class, properties = {"inviteId", "inviteName", "telNo", "role", "status", "cancelledAt", "expireTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("invite/invitation/cancel")
@Scope(ScopeTypes.PROTOTYPE)
public class Cancel implements com.th4.edge.tenant.user.service.invite.invitation.cancel.Cancel {
    @User private com.th4.edge.framework.provider.security.User<?> principal;
    @AutoInject @NotNull private Long inviteId;
    @AutoInject @Length(max = 500) private String reason;
    private final TenantContext tenantContext;
    private final MembershipContext membershipContext;
    public Cancel(TenantContext tenantContext, MembershipContext membershipContext) {
        this.tenantContext = tenantContext;
        this.membershipContext = membershipContext;
    }
    @Override @Transactional
    public Invite call(Employee request) {
        if (!(principal instanceof Employee employee)) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号尚未加入企业，请先接受企业邀请"));
        }
        return membershipContext.cancel(tenantContext.requireEmployee(employee), inviteId, reason);
    }
}
