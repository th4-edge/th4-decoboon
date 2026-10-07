package com.th4.edge.decoboon.project.service.contractchange.info.support;

import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
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
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 合同变更单详情实现。
 *
 * <p>一次返回变更单、变更明细、原合同行与合同金额上下文，避免前端按项目列表回读后过滤。
 * 成本变化按角色裁剪：无成本权限的角色响应中不返回 costDelta（BR-003）。</p>
 */
@Note("合同变更单详情")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "property", "no", "reason", "type", "sourceLine",
                        "amount", "costDelta", "status", "customerConfirmed", "customerConfirmedAt", "customerConfirmedBy", "customerConfirmMethod",
                        "evidenceImages", "signatureUrl", "items", "requestedBy", "approvedBy",
                        "conclusion", "approvedAt", "createTime"}),
        @AllowProperties(model = ContractChangeItem.class,
                properties = {"changeItemId", "sourceLine", "changeType", "itemType", "name",
                        "oldQuantity", "newQuantity", "oldPrice", "newPrice", "amountDelta", "costDelta", "remark"}),
        @AllowProperties(model = ContractLine.class,
                properties = {"lineId", "type", "category", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "amount", "cost", "spaceName", "sort"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "signedAt"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "status", "property"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract-change/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.project.service.contractchange.info.Info {

    @AutoInject
    @NotNull
    private Long changeId;

    @User
    private Employee employee;

    private final ContractChangeRepository changeRepository;
    private final ContractChangeItemRepository itemRepository;
    private final ProjectAccessContext accessContext;
    private final CatalogCostContext costContext;

    public Info(ContractChangeRepository changeRepository,
                ContractChangeItemRepository itemRepository,
                ProjectAccessContext accessContext,
                CatalogCostContext costContext) {
        this.changeRepository = changeRepository;
        this.itemRepository = itemRepository;
        this.accessContext = accessContext;
        this.costContext = costContext;
    }

    @Override
    @Transactional(readOnly = true)
    public ContractChange call(ContractChange request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<ContractChange> byId = (root, query, builder) ->
                builder.equal(root.get("changeId"), changeId);
        ContractChange change = changeRepository.findOne(
                        byId.and(accessContext.visibleChild(employee, ContractChange.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "变更单不存在或无权访问")));

        List<ContractChangeItem> items = itemRepository
                .findByChange_ChangeIdOrderByChangeItemIdAsc(change.getChangeId());
        change.setItems(items);

        if (!costContext.canViewCost(current)) {
            change.setCostDelta(null);
            items.forEach(item -> item.setCostDelta(null));
        }
        return change;
    }
}
