package com.th4.edge.tenant.user.service.user.employee.page.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.service.annotation.ServiceName;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 当前企业员工选择器。
 *
 * <p>服务端完成关键字、岗位、状态、门店和员工 ID 过滤及数据库分页，禁止前端拉取全量后筛选。</p>
 */
@Note("员工分页")
@Description("仅查询当前登录员工所属企业、且处于当前账号数据范围内的成员")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.tenant.user.model.Credentials.class,
                properties = {"creId", "openId", "unionId", "phone", "status", "employee"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt"}),
        @AllowProperties(model = Company.class,
                properties = {"comId", "title", "name", "logo", "color"}),
        @AllowProperties(model = Store.class,
                properties = {"storeId", "name", "defaultStore", "color", "addr", "company"}),
        @AllowProperties(model = Team.class,
                properties = {"teamId", "name", "color", "store", "company"})
})
@Service("user/employee/page")
@ServiceName("page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.tenant.user.service.user.employee.page.Page {

    @User
    private Employee employee;

    private final TenantContext tenantContext;
    private final EmployeeRepository employeeRepository;
    private final DataScopeContext dataScopeContext;

    /** 当前页，前端从 1 开始。 */
    @AutoInject
    private Integer current;

    /** 每页数量，选择器默认 20、硬上限 50。 */
    @AutoInject
    private Integer size;

    @AutoInject
    private String keyword;

    /** 支持单角色或逗号分隔多角色，如 Boss,Pm。 */
    @AutoInject
    private String role;

    /** Active / Suspended / Resigned / all；不传时保持全状态兼容。 */
    @AutoInject
    private String status;

    @AutoInject
    private Long storeId;

    /** 已选员工按 ID 回读，仍受企业、角色和数据范围约束。 */
    @AutoInject
    private Long empId;

    /** 离职交接等场景排除当前对象本人。 */
    @AutoInject
    private Long excludeEmpId;

    public Page(TenantContext tenantContext, EmployeeRepository employeeRepository,
                DataScopeContext dataScopeContext) {
        this.tenantContext = tenantContext;
        this.employeeRepository = employeeRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Employee> call(Employee request) {
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        Employee currentEmployee = tenantContext.requireRole(employee,
                EmployeeRole.Boss, EmployeeRole.StoreManager,
                EmployeeRole.Designer, EmployeeRole.Pm, EmployeeRole.Finance);
        DataScope scope = dataScopeContext.resolve(currentEmployee);
        // USER-DEC-20261029-009：财务可转交客户负责人，候选人范围必须与同源数据范围一致。
        // 总部财务按企业范围；门店财务与店长按本门店范围。
        boolean storeScoped = currentEmployee.getRole() == EmployeeRole.StoreManager
                || (currentEmployee.getRole() == EmployeeRole.Finance && scope.type() == DataScopeType.Store);
        Long effectiveStoreId = storeId;
        if (storeScoped) {
            if (scope.storeId() == null) {
                throw new DefinedException(new DefaultErrorCode("403", "当前账号未配置有效门店范围"));
            }
            if (storeId != null && !storeId.equals(scope.storeId())) {
                throw new DefinedException(new DefaultErrorCode("403", "无权查看其他门店员工"));
            }
            effectiveStoreId = scope.storeId();
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        String keywordPattern = normalizedKeyword.isBlank() ? null : "%" + normalizedKeyword + "%";
        return employeeRepository.findPickerPage(
                currentEmployee.getCompany().getComId(),
                effectiveStoreId,
                roleFilter(role),
                statusFilter(status),
                empId,
                excludeEmpId,
                keywordPattern,
                PageRequest.of(page, pageSize));
    }

    private List<EmployeeRole> roleFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return Arrays.asList(EmployeeRole.values());
        }
        try {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .map(EmployeeRole::valueOf)
                    .distinct()
                    .toList();
        } catch (IllegalArgumentException ignored) {
            throw new DefinedException(new DefaultErrorCode("400", "员工岗位筛选值无效"));
        }
    }

    private List<EmployeeStatus> statusFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return Arrays.asList(EmployeeStatus.values());
        }
        try {
            return List.of(EmployeeStatus.valueOf(value.trim()));
        } catch (IllegalArgumentException ignored) {
            throw new DefinedException(new DefaultErrorCode("400", "员工状态筛选值无效"));
        }
    }
}
