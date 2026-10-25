package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 费用/报销的服务端角色、数据范围、归属和防自审校验。 */
@Component
public class ExpenseAccessContext {

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final ExpenseRepository expenseRepository;
    private final ProjectAccessContext projectAccessContext;
    private final StoreRepository storeRepository;
    private final EmployeeRepository employeeRepository;

    public ExpenseAccessContext(TenantContext tenantContext,
                                DataScopeContext dataScopeContext,
                                ExpenseRepository expenseRepository,
                                ProjectAccessContext projectAccessContext,
                                StoreRepository storeRepository,
                                EmployeeRepository employeeRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.expenseRepository = expenseRepository;
        this.projectAccessContext = projectAccessContext;
        this.storeRepository = storeRepository;
        this.employeeRepository = employeeRepository;
    }

    public Employee requireSubmitter(Employee principal) {
        return tenantContext.requireEmployee(principal);
    }

    public Employee requireCategoryEditor(Employee principal) {
        return tenantContext.requireInternalRole(principal,
                EmployeeRole.StoreManager, EmployeeRole.Finance);
    }

    public Employee requireApprover(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.StoreManager);
    }

    public DataScope scope(Employee principal) {
        return dataScopeContext.resolve(tenantContext.requireEmployee(principal));
    }

    public Specification<Expense> visible(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        if (current.getRole() == EmployeeRole.Boss) {
            Long comId = current.getCompany().getComId();
            return (root, query, builder) -> builder.equal(root.get("company").get("comId"), comId);
        }
        return DataScopeSpecification.visible(Expense.class, dataScopeContext.resolve(current));
    }

    public Expense requireVisible(Employee principal, Long expenseId) {
        Employee current = tenantContext.requireEmployee(principal);
        if (expenseId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "费用单不能为空"));
        }
        return expenseRepository.findOne(((Specification<Expense>) (root, query, builder) ->
                        builder.equal(root.get("expenseId"), expenseId))
                .and(visible(current)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "费用单不存在或无权访问")));
    }

    public Project resolveProject(Employee current, Long projectId) {
        if (projectId == null || projectId == 0L) return null;
        return projectAccessContext.requireProject(projectId, current);
    }

    public Employee resolveOwner(Employee current, Long employeeId) {
        if (employeeId == null || Objects.equals(employeeId, current.getEmpId())) {
            return current;
        }
        if (current.getRole() == EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "财务不能代他人提交费用或报销"));
        }
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号无权代他人提交"));
        }
        Employee target = employeeRepository.findByEmpIdAndCompany_ComId(
                employeeId, current.getCompany().getComId());
        if (target == null || target.getStatus() != EmployeeStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("404", "费用归属员工不存在或已停用"));
        }
        if (current.getRole() == EmployeeRole.StoreManager
                && (current.getStore() == null || target.getStore() == null
                || !current.getStore().getStoreId().equals(target.getStore().getStoreId()))) {
            throw new DefinedException(new DefaultErrorCode("403", "店长只能代本店员工提交"));
        }
        return target;
    }

    public Store resolveStore(Employee current, Long requestedStoreId, Project project) {
        Store currentStore = current.getStore();
        if (project != null && project.getStore() != null) {
            return project.getStore();
        }
        Store store = currentStore;
        if (requestedStoreId != null
                && !Objects.equals(requestedStoreId, currentStore == null ? null : currentStore.getStoreId())) {
            DataScope scope = dataScopeContext.resolve(current);
            boolean companyScope = current.getRole() == EmployeeRole.Boss
                    || (current.getRole() == EmployeeRole.Finance && scope.type() == DataScopeType.Company);
            if (!companyScope) {
                throw new DefinedException(new DefaultErrorCode("403", "当前账号不能选择其他门店"));
            }
            if (requestedStoreId == 0L) {
                return null;
            }
            store = storeRepository.findByStoreId(requestedStoreId);
        }
        if (store == null || store.getCompany() == null
                || !current.getCompany().getComId().equals(store.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "门店不存在或无权访问"));
        }
        return store;
    }

    public Employee resolvePayee(Employee current, Employee owner, Long payeeEmployeeId,
                                 com.th4.edge.decoboon.finance.configure.ExpensePaymentMode mode) {
        if (mode == com.th4.edge.decoboon.finance.configure.ExpensePaymentMode.Reimbursement) {
            if (payeeEmployeeId != null && !Objects.equals(payeeEmployeeId, owner.getEmpId())) {
                throw new DefinedException(new DefaultErrorCode("400", "报销收款员工必须等于费用归属员工"));
            }
            return owner;
        }
        if (payeeEmployeeId == null) return null;
        Employee payee = employeeRepository.findByEmpIdAndCompany_ComId(
                payeeEmployeeId, current.getCompany().getComId());
        if (payee == null || payee.getStatus() != EmployeeStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("404", "收款员工不存在或已停用"));
        }
        return payee;
    }

    public void applyOwnership(Expense expense, Employee current, Employee owner, Project project, Store store) {
        expense.setCompany(current.getCompany());
        expense.setStore(store);
        expense.setTeam(current.getTeam());
        expense.setProject(project);
        expense.setRequester(current);
        expense.setOwner(owner);
    }

    public void requireOwnerOrManager(Expense expense, Employee current) {
        if (expense.getOwner() != null && Objects.equals(expense.getOwner().getEmpId(), current.getEmpId())) {
            return;
        }
        if (current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.StoreManager) {
            return;
        }
        throw new DefinedException(new DefaultErrorCode("403", "当前账号无权修改该费用单"));
    }

    public boolean isCompanyScope(Employee current) {
        DataScope scope = dataScopeContext.resolve(current);
        return current.getRole() == EmployeeRole.Boss || scope.type() == DataScopeType.Company;
    }
}
