package com.th4.edge.tenant.user.service.invite.invitation.pending.support;

import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.decoboon.customer.repository.CustomerInviteRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.configure.EntryInviteType;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.model.PendingEntryInvite;
import com.th4.edge.tenant.user.repository.InviteRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 登录后按已验证手机号发现待处理邀请。 */
@Note("登录后待处理邀请查询")
@Properties(allow = {
        @AllowProperties(model = PendingEntryInvite.class,
                properties = {"type", "inviteId", "companyName", "companyLogo", "companyColor", "inviterName",
                        "role", "storeName", "projectName", "propertyAddress", "customerName", "maskedPhone",
                        "status", "expireTime"})
})
@Service("invite/invitation/pending")
@Scope(ScopeTypes.PROTOTYPE)
public class Pending implements com.th4.edge.tenant.user.service.invite.invitation.pending.Pending {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    private final UserIdentityContext userContext;
    private final InviteRepository inviteRepository;
    private final CustomerInviteRepository customerInviteRepository;

    public Pending(UserIdentityContext userContext,
                   InviteRepository inviteRepository,
                   CustomerInviteRepository customerInviteRepository) {
        this.userContext = userContext;
        this.inviteRepository = inviteRepository;
        this.customerInviteRepository = customerInviteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingEntryInvite> call(Credentials request) {
        if (principal instanceof Employee) return List.of();
        if (principal == null || principal.getUserId() == null) {
            throw new DefinedException(new DefaultErrorCode("401", "登录状态已失效"));
        }
        Credentials credential = userContext.requireUser(Long.valueOf(principal.getUserId().toString()));
        if (credential.getPhone() == null || credential.getPhone().isBlank()) return List.of();
        if (userContext.activeEmployee(credential) != null) return List.of();

        LocalDateTime now = LocalDateTime.now();
        List<PendingEntryInvite> result = new ArrayList<>();
        for (Invite invite : inviteRepository.findByTelNoOrderByInviteIdDesc(credential.getPhone())) {
            if (invite.getStatus() != InviteStatus.Pending) continue;
            if (invite.getExpireTime() != null && !invite.getExpireTime().isAfter(now)) continue;
            PendingEntryInvite item = new PendingEntryInvite();
            item.setType(EntryInviteType.Employee);
            item.setInviteId(invite.getInviteId());
            item.setCompanyName(invite.getCompany().getTitle() != null
                    ? invite.getCompany().getTitle() : invite.getCompany().getName());
            item.setCompanyLogo(invite.getCompany().getLogo());
            item.setCompanyColor(invite.getCompany().getColor());
            item.setInviterName(invite.getInviter() == null ? null : invite.getInviter().getName());
            item.setRole(invite.getRole());
            item.setStoreName(invite.getStore() == null ? null : invite.getStore().getName());
            item.setMaskedPhone(mask(invite.getTelNo()));
            item.setStatus(invite.getStatus());
            item.setExpireTime(invite.getExpireTime());
            result.add(item);
        }
        for (CustomerInvite invite : customerInviteRepository.findByTelNoOrderByInviteIdDesc(credential.getPhone())) {
            if (invite.getStatus() != InviteStatus.Pending) continue;
            if (invite.getExpireTime() != null && !invite.getExpireTime().isAfter(now)) continue;
            PendingEntryInvite item = new PendingEntryInvite();
            item.setType(EntryInviteType.Customer);
            item.setInviteId(invite.getInviteId());
            item.setCompanyName(invite.getCompany().getTitle() != null
                    ? invite.getCompany().getTitle() : invite.getCompany().getName());
            item.setCompanyLogo(invite.getCompany().getLogo());
            item.setCompanyColor(invite.getCompany().getColor());
            item.setInviterName(invite.getInviter() == null ? null : invite.getInviter().getName());
            item.setCustomerName(invite.getCustomerName());
            Project project = invite.getProject();
            if (project != null) {
                item.setProjectName(project.getName());
                if (project.getProperty() != null) item.setPropertyAddress(project.getProperty().getAddress());
            }
            item.setMaskedPhone(mask(invite.getTelNo()));
            item.setStatus(invite.getStatus());
            item.setExpireTime(invite.getExpireTime());
            result.add(item);
        }
        return result;
    }

    private String mask(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String value = phone.trim();
        if (value.length() < 7) return "****";
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
