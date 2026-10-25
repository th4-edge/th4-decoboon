package com.th4.edge.tenant.user.service.company.member.page.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;
@Note("企业成员分页")
@Properties(allow = {
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "resignedAt", "resignReason", "createTime"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Credentials.class, properties = {"creId", "openId", "unionId", "phone", "status", "employee"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("company/member/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.tenant.user.service.company.member.page.Page {
    @User private Employee employee;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private EmployeeStatus status;
    @AutoInject private Long storeId;
    private final TenantContext tenantContext;
    private final EmployeeRepository employeeRepository;
    public Page(TenantContext tenantContext, EmployeeRepository employeeRepository) { this.tenantContext = tenantContext; this.employeeRepository = employeeRepository; }
    @Override
    public org.springframework.data.domain.Page<Employee> call(Employee request) {
        Employee currentEmployee = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.StoreManager);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        List<Employee> all = scoped(currentEmployee);
        if (storeId != null) {
            // 门店维度：在既有角色数据范围内进一步限定门店，不会突破 scoped 的权限边界
            all = all.stream()
                    .filter(item -> item.getStore() != null && storeId.equals(item.getStore().getStoreId()))
                    .toList();
        }
        List<Employee> filtered = status == null ? all : all.stream().filter(item -> item.getStatus() == status).toList();
        int from = Math.min(page * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        return new PageImpl<>(filtered.subList(from, to), PageRequest.of(page, pageSize), filtered.size());
    }
    private List<Employee> scoped(Employee current) {
        Long comId = current.getCompany().getComId();
        if (current.getRole() == EmployeeRole.StoreManager && current.getStore() != null) {
            return employeeRepository.findByCompany_ComIdAndStore_StoreIdOrderByName(comId, current.getStore().getStoreId());
        }
        return employeeRepository.findByCompany_ComId(comId);
    }
}
