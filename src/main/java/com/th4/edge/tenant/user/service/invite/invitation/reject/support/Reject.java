package com.th4.edge.tenant.user.service.invite.invitation.reject.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.model.Invite;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("拒绝邀请")
@Properties(allow = @AllowProperties(model = Invite.class,
        properties = {"inviteId", "inviteName", "telNo", "role", "status", "rejectedAt", "expireTime"}))
@Service("invite/invitation/reject")
@Scope(ScopeTypes.PROTOTYPE)
public class Reject implements com.th4.edge.tenant.user.service.invite.invitation.reject.Reject {
    @AutoInject @NotNull private Long inviteId;
    @AutoInject @NotEmpty @Length(max = 20) private String phone;
    @AutoInject @Length(max = 500) private String reason;
    private final MembershipContext membershipContext;
    public Reject(MembershipContext membershipContext) { this.membershipContext = membershipContext; }
    @Override @Transactional
    public Invite call(Invite request) { return membershipContext.reject(inviteId, phone, reason); }
}
