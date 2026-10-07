package com.th4.edge.decoboon.project.service.contractchange.customerconfirm.support;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.property.model.Property;
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

import java.util.List;

/** 记录客户对变更报价的确认。 */
@Note("客户确认变更")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "no", "type", "reason", "amount", "costDelta",
                        "status", "customerConfirmed", "customerConfirmedAt", "customerConfirmedBy", "customerConfirmMethod", "evidenceImages", "signatureUrl",
                        "items", "requestedBy", "approvedBy", "approvedAt", "createTime"}),
        @AllowProperties(model = ContractChangeItem.class,
                properties = {"changeItemId", "sourceLine", "changeType", "itemType", "name",
                        "oldQuantity", "newQuantity", "oldPrice", "newPrice", "amountDelta", "costDelta", "remark"}),
        @AllowProperties(model = ContractLine.class,
                properties = {"lineId", "type", "name", "quantity", "materialPrice", "manualPrice", "amount", "cost"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "signedAt"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "status", "property"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract-change/customer-confirm")
@Scope(ScopeTypes.PROTOTYPE)
public class Confirm implements com.th4.edge.decoboon.project.service.contractchange.customerconfirm.Confirm {
    @User private Employee employee;
    @AutoInject @NotNull private Long changeId;
    @AutoInject private String signatureUrl;
    @AutoInject private List<String> evidenceImages;
    private final ContractChangeRepository changeRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectContractContext contractContext;

    public Confirm(ContractChangeRepository changeRepository, ProjectAccessContext accessContext,
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
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Pm
                && current.getRole() != EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "当前角色不能登记客户确认"));
        }
        if (signatureUrl != null) change.setSignatureUrl(signatureUrl);
        if (evidenceImages != null) change.setEvidenceImages(evidenceImages);
        return contractContext.confirmCustomer(change, current);
    }
}
