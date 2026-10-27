package com.th4.edge.tenant.user.service.invite.invitation.page.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.InviteRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Note("邀请分页")
@Properties(allow = {
        @AllowProperties(model = Invite.class, properties = {"inviteId", "company", "store", "team", "inviter", "inviteName", "telNo", "role", "status", "expireTime", "acceptedAt", "cancelledAt", "rejectedAt", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("invite/invitation/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.tenant.user.service.invite.invitation.page.Page {
    @User private com.th4.edge.framework.provider.security.User<?> principal;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final TenantContext tenantContext;
    private final InviteRepository inviteRepository;
    public Page(TenantContext tenantContext, InviteRepository inviteRepository) { this.tenantContext = tenantContext; this.inviteRepository = inviteRepository; }
    @Override
    @Transactional
    public org.springframework.data.domain.Page<Invite> call(Employee request) {
        if (!(principal instanceof Employee employee)) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号尚未加入企业，请先接受企业邀请"));
        }
        Employee currentEmployee = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.StoreManager);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        org.springframework.data.domain.Page<Invite> result = inviteRepository.findByCompany_ComId(
                currentEmployee.getCompany().getComId(), PageRequest.of(page, pageSize));
        LocalDateTime now = LocalDateTime.now();
        List<Invite> expired = result.getContent().stream()
                .filter(invite -> invite.getStatus() == InviteStatus.Pending)
                .filter(invite -> invite.getExpireTime() != null && !invite.getExpireTime().isAfter(now))
                .peek(invite -> invite.setStatus(InviteStatus.Expired))
                .toList();
        if (!expired.isEmpty()) inviteRepository.saveAll(expired);
        return result;
    }
}
