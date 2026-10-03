package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * 客户归属上下文。
 *
 * <p>集中处理负责人校验以及 company/store/team/owner 归属快照，避免登记和编辑
 * Provider 各自复制权限组装逻辑。</p>
 */
@Note("客户归属上下文")
@Component
public class CustomerContext {

    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final DataScopeContext dataScopeContext;

    public CustomerContext(EmployeeRepository employeeRepository,
                           CustomerRepository customerRepository,
                           DataScopeContext dataScopeContext) {
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.dataScopeContext = dataScopeContext;
    }

    private static final Set<EmployeeRole> ASSIGNABLE_OWNER_ROLES = Set.of(
            EmployeeRole.Boss,
            EmployeeRole.StoreManager,
            EmployeeRole.TeamLeader,
            EmployeeRole.Marketer,
            EmployeeRole.Designer
    );

    /**
     * 按数据范围加载客户，并兼容设计师作为业务负责人使用客户档案的场景。
     *
     * <p>设计师既可访问自己负责的客户，也可访问已指派给自己的设计客户；
     * 其他角色仍严格沿用框架数据范围。</p>
     */
    public Customer requireVisible(Long customerId, Employee principal) {
        return findVisible(customerId, principal)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "客户不存在或无权访问")));
    }

    public Optional<Customer> findVisible(Long customerId, Employee principal) {
        if (customerId == null) return Optional.empty();
        Employee current = dataScopeContext.requireEmployee(principal);
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Customer> byId = (root, query, builder) ->
                builder.equal(root.get("custId"), customerId);
        Optional<Customer> scoped = customerRepository.findOne(
                byId.and(DataScopeSpecification.visible(Customer.class, scope)));
        if (scoped.isPresent() || current.getRole() != EmployeeRole.Designer) {
            return scoped;
        }
        // 不用带 PESSIMISTIC_WRITE 的客户查询：本方法同时服务只读详情，
        // 且上面的数据范围查询已经覆盖“本人是客户负责人”的情况。
        return customerRepository.findByCustIdAndCompany_ComIdAndDesigner_EmpId(
                customerId, requireCompanyId(current), current.getEmpId());
    }

    /**
     * 解析客户负责人。
     *
     * <p>企业负责人和店长可以明确转交客户；店长只能转交给本门店在职人员。
     * 其他角色沿用业务回退负责人，不能通过 ownerId 转交客户。</p>
     */
    public Employee resolveOwner(Employee current, Long ownerId, Employee fallback) {
        Long comId = requireCompanyId(current);
        if (ownerId != null
                && (current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.StoreManager)) {
            return resolveAssignableOwner(current, ownerId);
        }
        Employee owner = fallback == null ? current : fallback;
        if (owner.getCompany() == null || owner.getCompany().getComId() == null
                || !comId.equals(owner.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "负责人不属于当前企业"));
        }
        return owner;
    }

    /** 解析批量录入或客户转交时明确指定的负责人。 */
    public Employee resolveAssignableOwner(Employee current, Long ownerId) {
        Long comId = requireCompanyId(current);
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "仅企业负责人或店长可以变更客户负责人"));
        }
        if (ownerId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择客户负责人"));
        }
        Employee owner = employeeRepository.findByEmpIdAndCompany_ComId(ownerId, comId);
        if (owner == null || owner.getStatus() != EmployeeStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("400", "负责人不属于当前企业或已离职"));
        }
        if (!ASSIGNABLE_OWNER_ROLES.contains(owner.getRole())) {
            throw new DefinedException(new DefaultErrorCode("400", "客户负责人只能选择业务员、设计师、团队负责人、店长或企业负责人"));
        }
        if (current.getRole() == EmployeeRole.StoreManager) {
            if (current.getStore() == null || owner.getStore() == null
                    || !current.getStore().getStoreId().equals(owner.getStore().getStoreId())) {
                throw new DefinedException(new DefaultErrorCode("400", "仅可转交给本门店人员"));
            }
        }
        return owner;
    }

    private Long requireCompanyId(Employee current) {
        if (current == null || current.getCompany() == null
                || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        return current.getCompany().getComId();
    }

    /**
     * 从负责人复制客户归属快照，前端提交的 store/team/company 不参与落库。
     */
    public void applyOwnership(Customer customer, Employee owner) {
        if (owner == null || owner.getCompany() == null
                || owner.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "客户负责人未归属企业"));
        }
        Company company = owner.getCompany();
        customer.setCompany(company);
        customer.setStore(owner.getStore());
        customer.setTeam(owner.getTeam());
        customer.setOwner(owner);
    }

    private boolean sameEmployee(Employee target, Employee current) {
        return target != null && current != null && target.getEmpId() != null
                && target.getEmpId().equals(current.getEmpId());
    }
}
