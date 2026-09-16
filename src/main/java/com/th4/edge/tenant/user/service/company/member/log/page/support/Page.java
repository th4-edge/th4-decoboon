package com.th4.edge.tenant.user.service.company.member.log.page.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.CompanyMemberLog;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.CompanyMemberLogRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
@Note("成员变更日志分页")
@Properties(allow = {
        @AllowProperties(model = CompanyMemberLog.class, properties = {"logId", "user", "company", "employee", "action", "beforeSnapshot", "afterSnapshot", "operator", "reason", "createTime"}),
        @AllowProperties(model = Credentials.class, properties = {"creId", "openId", "unionId", "phone", "status"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role", "status"})
})
@Service("company/member/log/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.tenant.user.service.company.member.log.page.Page {
    @User private Employee employee;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final TenantContext tenantContext;
    private final CompanyMemberLogRepository logRepository;
    public Page(TenantContext tenantContext, CompanyMemberLogRepository logRepository) { this.tenantContext = tenantContext; this.logRepository = logRepository; }
    @Override
    public org.springframework.data.domain.Page<CompanyMemberLog> call(Employee request) {
        Employee currentEmployee = tenantContext.requireRole(employee, com.th4.edge.tenant.user.configure.EmployeeRole.Boss);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        return logRepository.findByCompany_ComIdOrderByCreateTime_DateTimeDesc(currentEmployee.getCompany().getComId(), PageRequest.of(page, pageSize));
    }
}
