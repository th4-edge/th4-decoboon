package com.th4.edge.decoboon.project.service.contractchange.update.support;

import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
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

/** 审批合同变更单实现。 */
@Note("审批合同变更")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "no", "reason", "type", "sourceLine",
                        "amount", "costDelta", "status", "customerConfirmed", "customerConfirmedAt",
                        "evidenceImages", "signatureUrl", "requestedBy", "approvedBy", "conclusion",
                        "approvedAt", "createTime"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "signedAt"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "contractAmount", "status", "property" }),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract-change/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.project.service.contractchange.update.Update {

    @AutoInject
    @NotNull
    private Long changeId;

    @AutoInject
    @NotNull
    private Boolean approved;

    @AutoInject
    private String conclusion;

    @User
    private Employee employee;

    private final ContractChangeRepository changeRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectContractContext contractContext;

    public Update(ContractChangeRepository changeRepository,
                  ProjectAccessContext accessContext,
                  ProjectContractContext contractContext) {
        this.changeRepository = changeRepository;
        this.accessContext = accessContext;
        this.contractContext = contractContext;
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
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人或财务可以审批合同变更"));
        }
        if (change.getStatus() != ContractChangeStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", "变更单已处理"));
        }
        return contractContext.approve(change, current, approved, conclusion);
    }
}
