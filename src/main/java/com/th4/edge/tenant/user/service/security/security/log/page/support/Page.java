package com.th4.edge.tenant.user.service.security.security.log.page.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.UserSecurityLog;
import com.th4.edge.tenant.user.repository.UserSecurityLogRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
@Note("安全日志分页")
@Properties(allow = @AllowProperties(model = UserSecurityLog.class,
        properties = {"logId", "userId", "openId", "phone", "companyId", "empId", "action", "result", "ip", "userAgent", "remark", "createTime"}))
@Service("security/security/log/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.tenant.user.service.security.security.log.page.Page {
    @User private Employee employee;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private String phone;
    private final TenantContext tenantContext;
    private final UserSecurityLogRepository logRepository;
    private final UserIdentityContext userContext;
    public Page(TenantContext tenantContext, UserSecurityLogRepository logRepository, UserIdentityContext userContext) { this.tenantContext = tenantContext; this.logRepository = logRepository; this.userContext = userContext; }
    @Override
    public org.springframework.data.domain.Page<UserSecurityLog> call(Employee request) {
        Employee currentEmployee = tenantContext.requireRole(employee, com.th4.edge.tenant.user.configure.EmployeeRole.Boss);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        PageRequest pageable = PageRequest.of(page, pageSize);
        if (phone != null && !phone.isBlank()) return logRepository.findByPhoneOrderByCreateTime_DateTimeDesc(phone, pageable);
        com.th4.edge.tenant.user.model.Credentials credential = userContext.requireWx(currentEmployee);
        if (credential == null) return org.springframework.data.domain.Page.empty(pageable);
        return logRepository.findByUserIdOrderByCreateTime_DateTimeDesc(credential.getCreId(), pageable);
    }
}
