package com.th4.edge.decoboon.project.service.contractchange.batchsave.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 一次事务保存一张包含多行增减项的合同变更单。 */
@Note("批量发起合同变更")
@Description("面向连续录入场景，一次提交多行增减项并汇总合同变更金额")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "no", "reason", "type",
                        "amount", "costDelta", "status", "customerConfirmed", "evidenceImages",
                        "signatureUrl", "items", "requestedBy", "createTime"}),
        @AllowProperties(model = ContractChangeItem.class,
                properties = {"changeItemId", "changeType", "itemType", "name", "oldQuantity", "newQuantity",
                        "oldPrice", "newPrice", "amountDelta", "costDelta", "remark"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "signedAt"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "status", "property"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract-change/batch-save")
@Scope(ScopeTypes.PROTOTYPE)
public class BatchSave implements com.th4.edge.decoboon.project.service.contractchange.batchsave.BatchSave {

    @AutoInject
    @NotNull
    private Long contractId;

    @AutoInject
    @NotEmpty
    private String reason;

    @AutoInject
    private ContractChangeType type;

    @AutoInject
    @NotEmpty
    private List<ContractChangeItem> items;

    @AutoInject
    private List<String> evidenceImages;

    @AutoInject
    private String signatureUrl;

    @User
    private Employee employee;

    private final ProjectContractRepository contractRepository;
    private final ContractChangeRepository changeRepository;
    private final ContractChangeItemRepository itemRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectPermissionContext permissionContext;
    private final ModelIdentityContext modelIdentityContext;

    public BatchSave(ProjectContractRepository contractRepository,
                     ContractChangeRepository changeRepository,
                     ContractChangeItemRepository itemRepository,
                     ProjectAccessContext accessContext,
                     ProjectPermissionContext permissionContext,
                     ModelIdentityContext modelIdentityContext) {
        this.contractRepository = contractRepository;
        this.changeRepository = changeRepository;
        this.itemRepository = itemRepository;
        this.accessContext = accessContext;
        this.permissionContext = permissionContext;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Override
    @Transactional
    public ContractChange call(ContractChange request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<ProjectContract> contractById = (root, query, builder) ->
                builder.equal(root.get("contractId"), contractId);
        ProjectContract contract = contractRepository.findOne(
                        contractById.and(accessContext.visibleContract(employee)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合同基线不存在或无权访问")));
        if (contract.getProject() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "合同尚未生成项目"));
        }
        Project project = accessContext.requireProject(contract.getProject().getProjectId(), current);
        accessContext.requirePlanEditor(project, current);

        ContractChangeType batchType = type == null ? ContractChangeType.Add : type;
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        List<ContractChangeItem> preparedItems = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            ContractChangeItem item = items.get(index);
            String name = item.getName() == null ? "" : item.getName().trim();
            if (name.isBlank()) {
                throw new DefinedException(new DefaultErrorCode("400", "第 " + (index + 1) + " 行项目名称不能为空"));
            }
            BigDecimal amountDelta = value(item.getAmountDelta());
            BigDecimal costDelta = value(item.getCostDelta());
            if (amountDelta.signum() == 0 && costDelta.signum() == 0) {
                throw new DefinedException(new DefaultErrorCode("400", "第 " + (index + 1) + " 行金额或成本不能同时为 0"));
            }
            if (batchType == ContractChangeType.Add && amountDelta.signum() < 0) {
                throw new DefinedException(new DefaultErrorCode("400", "增项金额不能为负数"));
            }
            if (batchType == ContractChangeType.Remove && amountDelta.signum() > 0) {
                throw new DefinedException(new DefaultErrorCode("400", "减项金额不能为正数"));
            }
            amount = amount.add(amountDelta);
            cost = cost.add(costDelta);

            item.setName(name);
            item.setChangeType(item.getChangeType() == null ? batchType : item.getChangeType());
            item.setOldQuantity(zero(item.getOldQuantity()));
            item.setNewQuantity(zero(item.getNewQuantity()));
            item.setOldPrice(zero(item.getOldPrice()));
            item.setNewPrice(zero(item.getNewPrice()));
            item.setAmountDelta(amountDelta);
            item.setCostDelta(costDelta);
            permissionContext.apply(item, project);
            preparedItems.add(item);
        }

        request.setCompany(project.getCompany());
        request.setContract(contract);
        request.setProject(project);
        request.setProperty(project.getProperty());
        permissionContext.apply(request, project);
        request.setNo("BC" + System.currentTimeMillis());
        request.setReason(reason.trim());
        request.setType(batchType);
        request.setAmount(amount);
        request.setCostDelta(cost);
        request.setStatus(ContractChangeStatus.Pending);
        request.setCustomerConfirmed(false);
        request.setRequestedBy(current);
        request.setEvidenceImages(evidenceImages);
        request.setSignatureUrl(signatureUrl);
        ContractChange saved = changeRepository.save(modelIdentityContext.prepare(request));

        for (ContractChangeItem item : preparedItems) {
            item.setChange(saved);
            itemRepository.save(modelIdentityContext.prepare(item));
        }
        saved.setItems(preparedItems);
        return saved;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
