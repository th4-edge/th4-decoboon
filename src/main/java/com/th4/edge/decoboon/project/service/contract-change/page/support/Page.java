package com.th4.edge.decoboon.project.service.contractchange.page.support;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/** 合同变更单查询实现。 */
@Note("合同变更单查询")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "no", "reason", "type", "sourceLine",
                        "amount", "costDelta", "status", "customerConfirmed", "customerConfirmedAt", "customerConfirmedBy", "customerConfirmMethod",
                        "evidenceImages", "signatureUrl", "items", "requestedBy", "approvedBy",
                        "conclusion", "approvedAt", "createTime"}),
        @AllowProperties(model = ContractChangeItem.class,
                properties = {"changeItemId", "sourceLine", "changeType", "itemType", "name",
                        "oldQuantity", "newQuantity", "oldPrice", "newPrice", "amountDelta", "costDelta", "remark"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "signedAt"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "contractAmount", "status", "property"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.ContractLine.class, properties = {"lineId", "name", "type", "category", "brand", "spec", "unit", "quantity", "materialPrice", "manualPrice", "amount"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class, properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"})
})
@Service("project/contract-change/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.contractchange.page.Page {
    @AutoInject private Long projectId;
    @User private Employee employee;
    private final ProjectRepository projectRepository;
    private final ContractChangeRepository changeRepository;
    private final ContractChangeItemRepository itemRepository;
    private final ProjectAccessContext accessContext;

    public Page(ProjectRepository projectRepository,
                ContractChangeRepository changeRepository,
                ContractChangeItemRepository itemRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.changeRepository = changeRepository;
        this.itemRepository = itemRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<ContractChange> call(ContractChange request) {
        if (projectId == null) return new PageImpl<>(List.of());
        Project project = accessContext.requireProject(projectId, employee);
        List<ContractChange> changes = changeRepository
                .findByProject_ProjectIdAndCompany_ComIdOrderByChangeIdDesc(projectId, project.getCompany().getComId());
        changes.forEach(change -> change.setItems(
                itemRepository.findByChange_ChangeIdOrderByChangeItemIdAsc(change.getChangeId())));
        return new PageImpl<>(changes);
    }
}
