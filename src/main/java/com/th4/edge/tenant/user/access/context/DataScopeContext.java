package com.th4.edge.tenant.user.access.context;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeRolePolicy;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.stereotype.Component;

/**
 * 当前登录员工数据范围上下文。
 */
@Note("数据范围上下文")
@Component
public class DataScopeContext {

    private final TenantContext tenantContext;

    public DataScopeContext(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * 根据登录主体重新加载员工并解析数据范围。
     */
    public DataScope requireScope(Employee principal) {
        return resolve(tenantContext.requireEmployee(principal));
    }

    /**
     * 根据登录主体重新加载完整员工。
     */
    public Employee requireEmployee(Employee principal) {
        return tenantContext.requireEmployee(principal);
    }

    /**
     * 解析已加载员工的角色数据范围。
     */
    public DataScope resolve(Employee current) {
        if (current == null || current.getEmpId() == null) {
            throw new DefinedException(new DefaultErrorCode("401", "登录状态已失效"));
        }
        EmployeeRolePolicy.requireApplicationAccess(current);

        Company company = current.getCompany();
        if (company == null || company.getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }

        Store store = current.getStore();
        Team team = current.getTeam();
        DataScopeType type = current.getRole() == null
                ? DataScopeType.Self
                : current.getRole().getDataScopeType();
        // DEC-065 FIN-STORE-03：多门店企业保留「总部财务」，归属默认总店的财务按企业范围，
        // 承载跨店汇总、合并与公司经营利润；其余财务按归属门店（FIN-STORE-01/02）。
        if (type == DataScopeType.Store
                && current.getRole() == EmployeeRole.Finance
                && store != null
                && Boolean.TRUE.equals(store.getDefaultStore())) {
            type = DataScopeType.Company;
        }

        if (type == DataScopeType.Store && (store == null || store.getStoreId() == null)) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属门店"));
        }
        if (type == DataScopeType.Team && (team == null || team.getTeamId() == null)) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属团队"));
        }

        return new DataScope(
                type,
                company.getComId(),
                store == null ? null : store.getStoreId(),
                team == null ? null : team.getTeamId(),
                current.getEmpId()
        );
    }
}
