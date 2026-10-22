package com.th4.edge.tenant.user.context;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeRolePolicy;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

/**
 * 当前登录租户上下文。
 */
@Note("当前租户上下文")
@Component
public class TenantContext {

    private final EmployeeRepository employeeRepository;

    public TenantContext(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * 根据 Provider 注入的当前登录主体加载完整租户员工。
     */
    public Employee requireEmployee(Employee principal) {
        if (principal == null || principal.getEmpId() == null) {
            throw new DefinedException(new DefaultErrorCode("401", "登录状态已失效"));
        }
        Employee employee = employeeRepository.findByEmpId(principal.getEmpId());
        if (employee == null) {
            throw new DefinedException(new DefaultErrorCode("401", "当前员工不存在"));
        }
        if (employee.getStatus() != EmployeeStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工已离职或停用"));
        }
        EmployeeRolePolicy.requireApplicationAccess(employee);
        return employee;
    }

    /**
     * 当前企业负责人。
     *
     * <p>邀请、成员管理等租户级动作只允许 BOSS 执行，服务端不能依赖前端按钮隐藏。</p>
     */
    public Employee requireBoss(Employee principal) {
        return requireRole(principal, EmployeeRole.Boss);
    }

    /**
     * 当前员工必须属于指定角色之一。
     *
     * <p>该方法是精确角色校验，保留给租户治理、专项报表等必须由特定角色承担的场景。</p>
     */
    public Employee requireRole(Employee principal, EmployeeRole... roles) {
        Employee employee = requireEmployee(principal);
        for (EmployeeRole role : roles) {
            if (employee.getRole() == role) {
                return employee;
            }
        }
        throw new DefinedException(new DefaultErrorCode("403", "当前账号无权执行该操作"));
    }

    /**
     * 当前员工必须具备指定内部岗位能力。
     *
     * <p>依据 DEC-032 / BR-028，企业负责人 BOSS 是本企业全部内部岗位能力的超集。
     * 该方法只用于企业内部能力入口，不适用于 requireBoss 等租户治理专属动作，
     * 也不适用于业主、客户等外部主体确认。</p>
     */
    public Employee requireInternalRole(Employee principal, EmployeeRole... roles) {
        Employee employee = requireEmployee(principal);
        if (employee.getRole() == EmployeeRole.Boss) {
            return employee;
        }
        for (EmployeeRole role : roles) {
            if (employee.getRole() == role) {
                return employee;
            }
        }
        throw new DefinedException(new DefaultErrorCode("403", "当前账号无权执行该操作"));
    }

    /**
     * 当前员工所属企业。
     */
    public Company requireCompany(Employee principal) {
        Company company = requireEmployee(principal).getCompany();
        if (company == null) {
            throw new DefinedException(new DefaultErrorCode("400", "当前员工未归属企业"));
        }
        return company;
    }
}
