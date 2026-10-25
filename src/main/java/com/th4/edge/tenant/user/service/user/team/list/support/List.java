package com.th4.edge.tenant.user.service.user.team.list.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.repository.TeamRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 当前账号可见团队列表。
 *
 * <p>Team 已冻结：本接口仅保留历史只读兼容。BOSS 可见企业历史 Team，店长可见本店历史 Team；不再作为管理入口或授权依据。</p>
 */
@Note("团队列表")
@Properties(allow = {
        @AllowProperties(model = Team.class,
                properties = {"teamId", "name", "color", "leader", "store", "company", "createTime"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name", "telNo", "role"}),
        @AllowProperties(model = Store.class,
                properties = {"storeId", "name", "defaultStore"}),
        @AllowProperties(model = Company.class,
                properties = {"comId", "title", "name"})
})
@Service("user/team/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.tenant.user.service.user.team.list.List {

    @User
    private Employee employee;

    private final TenantContext tenantContext;
    private final TeamRepository teamRepository;

    public List(TenantContext tenantContext, TeamRepository teamRepository) {
        this.tenantContext = tenantContext;
        this.teamRepository = teamRepository;
    }

    @Override
    public java.util.List<Team> call(Employee request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.StoreManager);
        Long comId = current.getCompany().getComId();
        if (current.getRole() == EmployeeRole.Boss) {
            return teamRepository.findByCompany_ComIdOrderByName(comId);
        }
        if (current.getStore() == null || current.getStore().getStoreId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前店长未归属门店"));
        }
        return teamRepository.findByCompany_ComIdAndStore_StoreIdOrderByName(
                comId, current.getStore().getStoreId());
    }
}
