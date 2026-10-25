package com.th4.edge.tenant.user.service.invite.invitation.save.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Note("创建邀请")
@Properties(allow = {
        @AllowProperties(model = Invite.class, properties = {"inviteId", "company", "store", "team", "inviter", "inviteName", "telNo", "role", "status", "expireTime", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("invite/invitation/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.tenant.user.service.invite.invitation.save.Save {
    @User private Employee employee;
    @AutoInject @Length(max = 15) private String inviteName;
    @AutoInject @Length(max = 20) private String invitePhone;
    @AutoInject private EmployeeRole role;
    /** 财务邀请目标门店：BOSS 必填；店长只能传本店或不传。 */
    @AutoInject private Long storeId;
    @AutoInject private Long teamId;
    @AutoInject @Length(max = 128) private String teamName;
    private final TenantContext tenantContext;
    private final MembershipContext membershipContext;
    public Save(TenantContext tenantContext, MembershipContext membershipContext) { this.tenantContext = tenantContext; this.membershipContext = membershipContext; }
    @Override @Transactional
    public Invite call(Employee request) { return membershipContext.createInvite(tenantContext.requireEmployee(employee), inviteName, invitePhone, role, storeId, teamId, teamName); }
}
