package com.th4.edge.tenant.user.service.invite.invitation.info.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("邀请详情")
@Properties(allow = {
        @AllowProperties(model = Invite.class, properties = {"inviteId", "company", "store", "team", "inviter",
                "inviteName", "maskedTelNo", "role", "status", "expireTime", "acceptedAt", "cancelledAt", "rejectedAt", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "addr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("invite/invitation/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.tenant.user.service.invite.invitation.info.Info {
    @AutoInject @NotNull private Long inviteId;
    @AutoInject private String phone;
    private final MembershipContext membershipContext;
    public Info(MembershipContext membershipContext) { this.membershipContext = membershipContext; }
    @Override @Transactional
    public Invite call(Invite request) {
        Invite invite = membershipContext.requireInvite(inviteId);
        if (phone != null && !phone.isBlank() && !phone.equals(invite.getTelNo())) {
            throw new DefinedException(new DefaultErrorCode("400", "手机号与邀请不一致"));
        }
        invite.setMaskedTelNo(mask(invite.getTelNo()));
        return invite;
    }

    private String mask(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String value = phone.trim();
        if (value.length() < 7) return "****";
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
