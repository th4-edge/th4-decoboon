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
 * <p>BOSS 可见企业全部团队，店长可见本门店团队，团队负责人仅可见本人团队。</p>
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
        Employee current = tenantContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        return switch (current.getRole()) {
            case Boss -> teamRepository.findByCompany_ComIdOrderByName(comId);
            case StoreManager -> {
                if (current.getStore() == null || current.getStore().getStoreId() == null) {
                    throw new DefinedException(new DefaultErrorCode("403", "当前店长未归属门店"));
                }
                yield teamRepository.findByCompany_ComIdAndStore_StoreIdOrderByName(
                        comId, current.getStore().getStoreId());
            }
            case TeamLeader -> {
                Team team = current.getTeam();
                if (team == null || team.getTeamId() == null) {
                    throw new DefinedException(new DefaultErrorCode("403", "当前团队负责人未归属团队"));
                }
                Team visible = teamRepository.findByTeamIdAndCompany_ComId(team.getTeamId(), comId);
                yield visible == null ? java.util.List.of() : java.util.List.of(visible);
            }
            default -> throw new DefinedException(new DefaultErrorCode("403", "当前账号无权查看团队"));
        };
    }
}
