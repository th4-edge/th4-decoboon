package com.th4.edge.decoboon.project.service.contractchange.preview.support;

import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.project.service.contractchange.batchsave.support.BatchSave;
import com.th4.edge.framework.core.ScopeTypes;
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

import java.util.List;

/**
 * 增减项草稿预览。
 *
 * <p>与 batch-save 共用 {@link BatchSave#prepareDraft}，保证预览和正式提交的金额、
 * 当前有效剩余、目标快照和阻断规则完全同源，但本 Provider 不执行任何写库。</p>
 */
@Note("预览增减项草稿")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"contract", "project", "type", "amount", "costDelta", "status", "items"}),
        @AllowProperties(model = ContractChangeItem.class,
                properties = {"changeItemId", "sourceLineId", "sourceChangeItemId", "targetCatalogItemId",
                        "reduceQuantity", "changeType", "itemType", "name", "oldQuantity", "newQuantity",
                        "oldPrice", "newPrice", "amountDelta", "costDelta", "remark",
                        "sourceRemainingQuantity", "sourceRemainingAmount", "sourceUnitPrice", "sourceBaselineTime",
                        "targetName", "targetType", "targetCategory", "targetBrand", "targetSeries",
                        "targetSpec", "targetUnit", "targetPricingMode", "targetMaterialPrice",
                        "targetManualPrice", "targetFixedTotalPrice", "targetFormula", "targetCraft",
                        "targetCriterion", "targetQuantity", "targetAmount", "targetCostPrice",
                        "targetCostAmount", "targetCostComplete"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "signedAt"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "status", "property"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract-change/preview")
@Scope(ScopeTypes.PROTOTYPE)
public class Preview implements com.th4.edge.decoboon.project.service.contractchange.preview.Preview {

    @AutoInject
    @NotNull
    private Long contractId;

    /** 兼容批量旧请求；新页面每行显式传 changeType。 */
    @AutoInject
    private ContractChangeType type;

    @AutoInject
    @NotEmpty
    private List<ContractChangeItem> items;

    @User
    private Employee employee;

    private final ProjectContractRepository contractRepository;
    private final ProjectAccessContext accessContext;
    private final BatchSave batchSave;

    public Preview(ProjectContractRepository contractRepository,
                   ProjectAccessContext accessContext,
                   BatchSave batchSave) {
        this.contractRepository = contractRepository;
        this.accessContext = accessContext;
        this.batchSave = batchSave;
    }

    @Override
    @Transactional(readOnly = true)
    public ContractChange call(ContractChange request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<ProjectContract> byId = (root, query, builder) ->
                builder.equal(root.get("contractId"), contractId);
        ProjectContract contract = contractRepository.findOne(
                        byId.and(accessContext.visibleContract(employee)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合同基线不存在或无权访问")));
        if (contract.getProject() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "合同尚未生成项目"));
        }
        Project project = accessContext.requireProject(contract.getProject().getProjectId(), current);
        accessContext.requirePlanEditor(project, current);
        return batchSave.prepareDraft(contract, project, type, items);
    }
}
