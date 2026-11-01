package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.CommissionRelease;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.model.CommissionAdjustment;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.model.Payslip;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;

/** 员工薪酬权限、组织归属和项目归属上下文。 */
@Component
public class PayrollAccessContext {

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final PayrollItemRepository payrollItemRepository;

    public PayrollAccessContext(TenantContext tenantContext,
                                DataScopeContext dataScopeContext,
                                EmployeeRepository employeeRepository,
                                ProjectRepository projectRepository,
                                PayrollItemRepository payrollItemRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.payrollItemRepository = payrollItemRepository;
    }

    /** 薪酬管理读取：BOSS、财务、店长。 */
    public Employee requireManager(Employee principal) {
        return tenantContext.requireRole(principal,
                EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.StoreManager);
    }

    /** 是否为薪酬管理读取角色；员工本人自助视角不是管理视角。 */
    public boolean isPayrollManager(Employee employee) {
        return employee != null && (employee.getRole() == EmployeeRole.Boss
                || employee.getRole() == EmployeeRole.Finance
                || employee.getRole() == EmployeeRole.StoreManager);
    }

    /**
     * 月度考勤查看：管理角色保持企业/门店范围，其他员工只能进入本人视角。
     * 非管理员工的 DataScope 必须解析为 Self，禁止借由客户端参数扩大范围。
     */
    public Employee requireAttendanceViewer(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        if (isPayrollManager(current)) {
            return current;
        }
        DataScope scope = dataScopeContext.resolve(current);
        if (scope.type() != DataScopeType.Self) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未配置有效考勤数据范围"));
        }
        return current;
    }

    /** 提成查看：管理角色保持企业/门店范围，其他员工只能进入本人视角。 */
    public Employee requireCommissionViewer(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        if (isPayrollManager(current)) {
            return current;
        }
        DataScope scope = dataScopeContext.resolve(current);
        if (scope.type() != DataScopeType.Self) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未配置有效提成数据范围"));
        }
        return current;
    }

    /** 薪酬制单与复核：BOSS、财务；店长不获得方案、考勤、提成和工资单生成权限。 */
    public Employee requireEditor(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.Finance);
    }

    /** 工资单确认：BOSS 或本店店长；门店边界由 visibleSheets/requireStoreItems 强制。 */
    public Employee requireConfirmActor(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.StoreManager);
    }

    /** 工资单退回：BOSS 或本店店长。 */
    public Employee requireReturnActor(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.StoreManager);
    }

    /** 工资单草稿删除：BOSS、总部财务或门店财务/店长；门店边界由数据范围强制。 */
    public Employee requireDeleteActor(Employee principal) {
        return tenantContext.requireRole(principal,
                EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.StoreManager);
    }

    /** 员工工资/提成发放：BOSS、财务或本店店长。 */
    public Employee requirePaymentActor(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.StoreManager);
    }

    /** BOSS 专属能力保留；店长动作不得复用此方法。 */
    public Employee requireBoss(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss);
    }

    /**
     * 公司级工资单的整单动作：企业范围可操作全部工资项；门店范围必须保证
     * 整张工资单的工资项均属于本店。门店财务与店长均不得操作混合门店工资单。
     */
    public void requireStoreItemsInStore(Employee current, List<PayrollItem> items) {
        DataScope scope = dataScopeContext.resolve(current);
        if (scope.type() == DataScopeType.Company) {
            return;
        }
        if (scope.type() != DataScopeType.Store || scope.storeId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未配置有效门店数据范围"));
        }
        Long storeId = scope.storeId();
        boolean mixedStore = items != null && items.stream().anyMatch(item ->
                item.getStore() == null || !storeId.equals(item.getStore().getStoreId()));
        if (mixedStore) {
            throw new DefinedException(new DefaultErrorCode("403", "工资单包含其他门店工资项，不能由当前门店账号整单操作"));
        }
    }


    /** 员工本人工资条、提成和考勤。 */
    public Employee requireSelf(Employee principal) {
        return tenantContext.requireEmployee(principal);
    }

    public Employee resolveEmployee(Employee principal, Long empId) {
        Employee current = requireManager(principal);
        Employee target = employeeRepository.findByEmpIdAndCompany_ComId(empId, current.getCompany().getComId());
        if (target == null) {
            throw new DefinedException(new DefaultErrorCode("404", "员工不存在或无权访问"));
        }
        DataScope scope = dataScopeContext.resolve(current);
        if (scope.type() == DataScopeType.Store) {
            Long currentStoreId = scope.storeId();
            Long targetStoreId = target.getStore() == null ? null : target.getStore().getStoreId();
            if (currentStoreId == null || !currentStoreId.equals(targetStoreId)) {
                throw new DefinedException(new DefaultErrorCode("404", "员工不存在或无权访问"));
            }
        } else if (scope.type() != DataScopeType.Company) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未配置有效薪酬数据范围"));
        }
        return target;
    }

    public Project resolveProject(Employee principal, Long projectId) {
        Employee current = requireEditor(principal);
        return projectRepository.findByProjectIdAndCompany_ComId(projectId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问")));
    }

    /**
     * 工资单是公司级单据（com_id + period 唯一），单店视图不能只按工资单头 store 过滤。
     * 单店可见条件：工资单头归属该店，或工资单内存在该门店工资项。
     */
    public Specification<PayrollSheet> visibleSheets(Employee principal, Long storeFilter) {
        Employee current = requireManager(principal);
        Long comId = current.getCompany().getComId();
        Specification<PayrollSheet> companyScope = (root, query, cb) ->
                cb.equal(root.get("company").get("comId"), comId);
        if (storeFilter == null || storeFilter == 0L) {
            return companyScope;
        }
        List<Long> itemSheetIds = payrollItemRepository.findSheetIdsByStore(comId, storeFilter);
        return companyScope.and((root, query, cb) -> {
            var headerStore = cb.equal(root.get("store").get("storeId"), storeFilter);
            return itemSheetIds.isEmpty() ? headerStore : cb.or(headerStore, root.get("sheetId").in(itemSheetIds));
        });
    }

    /**
     * 工资单详情使用与列表相同的组织范围：总部范围看企业，门店范围看本店；
     * 公司级工资单仍按「单头归属本店或包含本店工资项」判定，避免猜 ID 越店读取。
     */
    public Specification<PayrollSheet> visibleSheets(Employee principal) {
        Employee current = requireManager(principal);
        DataScope scope = dataScopeContext.resolve(current);
        if (scope.type() == DataScopeType.Store) {
            if (scope.storeId() == null) {
                throw new DefinedException(new DefaultErrorCode("403", "当前账号未配置有效薪酬数据范围"));
            }
            return visibleSheets(current, scope.storeId());
        }
        if (scope.type() == DataScopeType.Company) {
            return visibleSheets(current, null);
        }
        throw new DefinedException(new DefaultErrorCode("403", "当前账号未配置有效薪酬数据范围"));
    }

    public <T> Specification<T> visible(Employee principal, Class<T> modelClass) {
        Employee current = tenantContext.requireEmployee(principal);
        return DataScopeSpecification.visible(modelClass, dataScopeContext.resolve(current));
    }

    public void applyOwnership(CompensationScheme target, Employee current, Employee employee) {
        target.setCompany(current.getCompany());
        target.setStore(employee.getStore());
        target.setTeam(employee.getTeam());
        target.setEmployee(employee);
        target.setCreatedBy(current);
    }

    public void applyOwnership(AttendanceSummary target, Employee current, Employee employee) {
        target.setCompany(current.getCompany());
        target.setStore(employee.getStore());
        target.setTeam(employee.getTeam());
        target.setEmployee(employee);
        target.setCreatedBy(current);
    }

    public void applyOwnership(CommissionItem target, Employee current, Employee employee, Project project) {
        target.setCompany(current.getCompany());
        target.setStore(employee.getStore());
        target.setTeam(employee.getTeam());
        target.setEmployee(employee);
        target.setProject(project);
        target.setCreatedBy(current);
    }

    public void applyOwnership(CommissionAdjustment target, Employee current, CommissionItem original) {
        target.setCompany(current.getCompany());
        target.setStore(original.getStore());
        target.setTeam(original.getTeam());
        target.setEmployee(original.getEmployee());
        target.setProject(original.getProject());
        target.setOriginalCommission(original);
        target.setCreatedBy(current);
    }

    public void applyOwnership(CommissionRelease target, Employee current, Employee employee,
                               CommissionItem commission, com.th4.edge.decoboon.finance.model.Receipt receipt) {
        target.setCompany(current.getCompany());
        target.setStore(employee.getStore());
        target.setTeam(employee.getTeam());
        target.setEmployee(employee);
        target.setCommissionItem(commission);
        target.setReceipt(receipt);
        target.setCreatedBy(current);
    }

    public void applyOwnership(PayrollItem target, Employee current, Employee employee,
                               PayrollSheet sheet, Project project) {
        target.setCompany(current.getCompany());
        target.setStore(employee.getStore());
        target.setTeam(employee.getTeam());
        target.setEmployee(employee);
        target.setSheet(sheet);
        target.setProject(project);
        target.setCreatedBy(current);
    }

    public void applyOwnership(Payslip target, Employee current, Employee employee, PayrollSheet sheet) {
        target.setCompany(current.getCompany());
        target.setStore(employee.getStore());
        target.setTeam(employee.getTeam());
        target.setEmployee(employee);
        target.setSheet(sheet);
        target.setCreatedBy(current);
    }

    public void applyCompanyOwnership(PayrollSheet target, Employee current) {
        target.setCompany(current.getCompany());
        target.setStore(current.getStore());
        target.setTeam(current.getTeam());
        target.setOwner(current);
        target.setCreatedBy(current);
    }
}
