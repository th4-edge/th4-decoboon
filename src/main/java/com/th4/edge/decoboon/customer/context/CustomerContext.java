package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

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

    public CustomerContext(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * 解析客户负责人。
     *
     * <p>非 BOSS 使用业务回退负责人；登记时回退当前员工，编辑时回退原负责人，
     * 因此非 BOSS 不能通过 ownerId 转交客户。BOSS 指定负责人时必须属于当前企业。</p>
     */
    public Employee resolveOwner(Employee current, Long ownerId, Employee fallback) {
        if (current == null || current.getCompany() == null
                || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        Long comId = current.getCompany().getComId();
        Employee owner = fallback == null ? current : fallback;
        if (current.getRole() == EmployeeRole.Boss && ownerId != null) {
            owner = employeeRepository.findByEmpIdAndCompany_ComId(ownerId, comId);
            if (owner == null) {
                throw new DefinedException(new DefaultErrorCode("400", "负责人不属于当前企业"));
            }
        }
        if (owner.getCompany() == null || owner.getCompany().getComId() == null
                || !comId.equals(owner.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "负责人不属于当前企业"));
        }
        return owner;
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
}
