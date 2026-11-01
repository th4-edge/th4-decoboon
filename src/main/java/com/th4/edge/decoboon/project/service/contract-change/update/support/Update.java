package com.th4.edge.decoboon.project.service.contractchange.update.support;

import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.context.ContractChangeRangeContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
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
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** 审批合同变更单实现。 */
@Note("审批合同变更")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "no", "reason", "type", "sourceLine",
                        "amount", "costDelta", "status", "customerConfirmed", "customerConfirmedAt", "customerConfirmedBy", "customerConfirmMethod",
                        "evidenceImages", "signatureUrl", "requestedBy", "approvedBy", "conclusion",
                        "approvedAt", "createTime"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "signedAt"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "contractAmount", "status", "property" }),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.ContractLine.class, properties = {"lineId", "name", "type", "category", "brand", "spec", "unit", "quantity", "materialPrice", "manualPrice", "amount"})
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
    private final ContractChangeItemRepository itemRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectContractContext contractContext;
    private final ContractChangeRangeContext rangeContext;

    public Update(ContractChangeRepository changeRepository,
                  ContractChangeItemRepository itemRepository,
                  ProjectAccessContext accessContext,
                  ProjectContractContext contractContext,
                  ContractChangeRangeContext rangeContext) {
        this.changeRepository = changeRepository;
        this.itemRepository = itemRepository;
        this.accessContext = accessContext;
        this.contractContext = contractContext;
        this.rangeContext = rangeContext;
    }

    @Override
    @Transactional
    public ContractChange call(ContractChange request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<ContractChange> byId = (root, query, builder) ->
                builder.equal(root.get("changeId"), changeId);
        ContractChange change = changeRepository.findOne(
                        byId.and(accessContext.visibleChild(current, ContractChange.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "变更单不存在或无权访问")));
        // DEC-076：BOSS/财务保留既有审批能力；店长可审批本店变更，数据范围仍由 visibleChild 强制。
        if (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.Finance
                && current.getRole() != EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人、财务或本店店长可以审批合同变更"));
        }
        if (change.getStatus() != ContractChangeStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", "变更单已处理"));
        }
        if (Boolean.TRUE.equals(approved)) {
            validateSourceBaseline(change);
        }
        return contractContext.approve(change, current, approved, conclusion);
    }

    /**
     * DEC-055 / BR-041：审批时重新校验来源基线。
     * 来源在设计单创建后发生变化时拒绝审批，要求撤回后按新基线重新发起，禁止静默改客户已确认金额。
     */
    private void validateSourceBaseline(ContractChange change) {
        List<ContractChangeItem> items = itemRepository.findByChange_ChangeIdOrderByChangeItemIdAsc(change.getChangeId());
        for (ContractChangeItem item : items) {
            if (item.getSourceRemainingQuantity() == null && item.getSourceRemainingAmount() == null) {
                continue;
            }
            ContractChangeRangeContext.RangeSnapshot snapshot;
            if (item.getSourceLine() != null) {
                snapshot = rangeContext.snapshot(change.getContract(), item.getSourceLine(), change.getChangeId());
            } else if (item.getSourceChangeItem() != null) {
                snapshot = rangeContext.snapshot(change.getContract(), item.getSourceChangeItem(), change.getChangeId());
            } else {
                continue;
            }
            if (!snapshot.confirmable()) {
                throw new DefinedException(new DefaultErrorCode("400", "来源当前有效剩余暂不可确认，请撤回后重做变更"));
            }
            if (notEqual(item.getSourceRemainingQuantity(), snapshot.effectiveRemainingQuantity())
                    || notEqual(item.getSourceRemainingAmount(), snapshot.effectiveRemainingAmount())) {
                throw new DefinedException(new DefaultErrorCode("400", "来源范围已变化，请撤回后按最新剩余重新发起变更"));
            }
        }
    }

    private boolean notEqual(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) != 0;
    }
}
