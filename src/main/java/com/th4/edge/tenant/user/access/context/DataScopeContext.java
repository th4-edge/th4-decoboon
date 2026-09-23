package com.th4.edge.tenant.user.access.context;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.configure.DataScopeType;
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

        Company company = current.getCompany();
        if (company == null || company.getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }

        Store store = current.getStore();
        Team team = current.getTeam();
        DataScopeType type = current.getRole() == null
                ? DataScopeType.Self
                : current.getRole().getDataScopeType();

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
