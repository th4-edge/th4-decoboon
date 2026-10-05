package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverProjectRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** 承接任务权限加载与归属校验。 */
@Note("承接任务权限")
@Component
public class CarryoverAccessContext {

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final CarryoverProjectRepository projectRepository;

    public CarryoverAccessContext(TenantContext tenantContext,
                                  DataScopeContext dataScopeContext,
                                  CarryoverProjectRepository projectRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.projectRepository = projectRepository;
    }

    public Employee requireBoss(Employee principal) {
        return tenantContext.requireBoss(principal);
    }

    public Employee requireEditor(Employee principal) {
        return tenantContext.requireEmployee(principal);
    }

    /** 双轨对账维护：仅企业负责人或财务。 */
    public Employee requireFinance(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人或财务可以维护双轨对账"));
        }
        return current;
    }

    public CarryoverProject requireItem(Employee principal, Long itemId) {
        Employee current = tenantContext.requireEmployee(principal);
        Specification<CarryoverProject> byId = (root, query, builder) ->
                builder.equal(root.get("itemId"), itemId);
        return projectRepository.findOne(byId.and(visible(current)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问")));
    }

    public CarryoverProject requireEditableItem(Employee principal, Long itemId) {
        Employee current = tenantContext.requireEmployee(principal);
        CarryoverProject item = requireItem(current, itemId);
        if (current.getRole() != EmployeeRole.Boss
                && (item.getOwner() == null || !current.getEmpId().equals(item.getOwner().getEmpId()))) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人或项目负责人可以编辑承接项目"));
        }
        if (!item.getStatus().editable()) {
            throw new DefinedException(new DefaultErrorCode("400", "当前承接项目已不能直接编辑"));
        }
        return item;
    }

    /** 财务口径可编辑：Boss / Finance，且承接项目仍处于可编辑状态。 */
    public CarryoverProject requireFinanceEditableItem(Employee principal, Long itemId) {
        Employee current = tenantContext.requireEmployee(principal);
        CarryoverProject item = requireItem(current, itemId);
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人或财务可以维护剩余收付款计划"));
        }
        if (!item.getStatus().editable()) {
            throw new DefinedException(new DefaultErrorCode("400", "当前承接项目已不能直接编辑"));
        }
        return item;
    }

    public Specification<CarryoverProject> visible(Employee current) {
        if (current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.Finance) {
            Long comId = current.getCompany().getComId();
            return (root, query, builder) -> builder.equal(root.get("company").get("comId"), comId);
        }
        DataScope scope = dataScopeContext.resolve(current);
        return DataScopeSpecification.visible(CarryoverProject.class, scope);
    }
}
