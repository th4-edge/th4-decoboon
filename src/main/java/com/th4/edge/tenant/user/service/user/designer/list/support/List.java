package com.th4.edge.tenant.user.service.user.designer.list.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Note("本企业设计师列表")
@Properties(allow = {
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role", "store"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("user/designer/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.tenant.user.service.user.designer.list.List {
    @User private Employee employee;
    private final TenantContext tenantContext;
    private final EmployeeRepository employeeRepository;

    public List(TenantContext tenantContext, EmployeeRepository employeeRepository) {
        this.tenantContext = tenantContext;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public java.util.List<Employee> call(Employee request) {
        Employee current = tenantContext.requireEmployee(employee);
        java.util.Collection<EmployeeRole> roles = java.util.List.of(EmployeeRole.Designer, EmployeeRole.Boss);
        if (current.getRole() == EmployeeRole.StoreManager && current.getStore() != null) {
            return active(employeeRepository.findByCompany_ComIdAndStore_StoreIdAndRoleInOrderByName(
                    current.getCompany().getComId(), current.getStore().getStoreId(), roles));
        }
        if (current.getRole() == EmployeeRole.TeamLeader && current.getTeam() != null) {
            return active(employeeRepository.findByCompany_ComIdAndTeam_TeamIdAndRoleInOrderByName(
                    current.getCompany().getComId(), current.getTeam().getTeamId(), roles));
        }
        return active(employeeRepository.findByCompany_ComIdAndRoleInOrderByName(
                current.getCompany().getComId(), roles));
    }

    private java.util.List<Employee> active(java.util.List<Employee> employees) {
        return employees.stream().filter(employee -> employee.getStatus() == EmployeeStatus.Active).toList();
    }
}
