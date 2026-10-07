package com.th4.edge.decoboon.project.service.contractchange.cancel.support;

import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 撤回合同变更单实现。
 *
 * <p>仅待审批（Pending）的变更单可撤回，且只允许发起人本人或同团队负责人操作。
 * 撤回不产生任何金额、应收、任务或材料副作用，仅将状态置为已取消。</p>
 */
@Note("撤回合同变更")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "no", "reason", "type", "amount", "costDelta",
                        "status", "customerConfirmed", "customerConfirmedAt", "customerConfirmedBy", "customerConfirmMethod",
                        "requestedBy", "approvedBy", "conclusion",
                        "approvedAt", "createTime"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "contractAmount", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract-change/cancel")
@Scope(ScopeTypes.PROTOTYPE)
public class Cancel implements com.th4.edge.decoboon.project.service.contractchange.cancel.Cancel {

    @AutoInject
    @NotNull
    private Long changeId;

    @User
    private Employee employee;

    private final ContractChangeRepository changeRepository;
    private final ProjectAccessContext accessContext;

    public Cancel(ContractChangeRepository changeRepository, ProjectAccessContext accessContext) {
        this.changeRepository = changeRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public ContractChange call(ContractChange request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<ContractChange> byId = (root, query, builder) ->
                builder.equal(root.get("changeId"), changeId);
        ContractChange change = changeRepository.findOne(
                        byId.and(accessContext.visibleChild(employee, ContractChange.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "变更单不存在或无权访问")));
        if (change.getStatus() != ContractChangeStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", "只有待审批的变更单可以撤回"));
        }
        if (!canCancel(current, change)) {
            throw new DefinedException(new DefaultErrorCode("403", "只有发起人本人或同团队负责人可以撤回变更单"));
        }
        change.setStatus(ContractChangeStatus.Cancelled);
        return changeRepository.save(change);
    }

    private boolean canCancel(Employee current, ContractChange change) {
        Employee requester = change.getRequestedBy();
        if (requester != null && Objects.equals(requester.getEmpId(), current.getEmpId())) {
            return true;
        }
        boolean teamLeader = current.getRole() == EmployeeRole.TeamLeader
                || current.getRole() == EmployeeRole.StoreManager
                || current.getRole() == EmployeeRole.Boss;
        if (!teamLeader) {
            return false;
        }
        if (current.getRole() == EmployeeRole.TeamLeader) {
            return current.getTeam() != null && change.getTeam() != null
                    && Objects.equals(current.getTeam().getTeamId(), change.getTeam().getTeamId());
        }
        return true;
    }
}
