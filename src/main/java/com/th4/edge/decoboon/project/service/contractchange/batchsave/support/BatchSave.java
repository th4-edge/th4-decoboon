package com.th4.edge.decoboon.project.service.contractchange.batchsave.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.context.CarryoverLockContext;
import com.th4.edge.decoboon.project.context.ContractChangeAmountContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 一次事务保存一张包含多行增减项的合同变更单。 */
@Note("批量发起合同变更")
@Description("面向连续录入场景，一次提交多行增减项并汇总合同变更金额")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "no", "reason", "type",
                        "amount", "costDelta", "status", "customerConfirmed", "customerConfirmedAt",
                        "customerConfirmedBy", "customerConfirmMethod", "evidenceImages",
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

    @AutoInject
    private LocalDateTime customerConfirmedAt;

    @AutoInject
    private String customerConfirmedBy;

    @AutoInject
    private String customerConfirmMethod;

    @User
    private Employee employee;

    private final ProjectContractRepository contractRepository;
    private final ContractChangeRepository changeRepository;
    private final ContractChangeItemRepository itemRepository;
    private final ProjectAccessContext accessContext;
    private final ContractChangeAmountContext amountContext;
    private final ProjectPermissionContext permissionContext;
    private final CarryoverLockContext carryoverLockContext;
    private final ModelIdentityContext modelIdentityContext;

    public BatchSave(ProjectContractRepository contractRepository,
                     ContractChangeRepository changeRepository,
                     ContractChangeItemRepository itemRepository,
                     ProjectAccessContext accessContext,
                     ContractChangeAmountContext amountContext,
                     ProjectPermissionContext permissionContext,
                     ModelIdentityContext modelIdentityContext,
                     CarryoverLockContext carryoverLockContext) {
        this.contractRepository = contractRepository;
        this.changeRepository = changeRepository;
        this.itemRepository = itemRepository;
        this.accessContext = accessContext;
        this.amountContext = amountContext;
        this.permissionContext = permissionContext;
        this.carryoverLockContext = carryoverLockContext;
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

        requireCustomerSignOff();

        ContractChangeType batchType = type == null ? ContractChangeType.Add : type;
        // 效率：一次按合同范围加载本批原合同行，避免逐行查库。
        List<Long> sourceLineIds = items.stream()
                .map(ContractChangeItem::getSourceLineId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ContractLine> sourceLines = amountContext.lines(contract, sourceLineIds);
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        List<ContractChangeItem> preparedItems = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            ContractChangeItem item = items.get(index);
            String name = item.getName() == null ? "" : item.getName().trim();
            if (name.isBlank()) {
                throw new DefinedException(new DefaultErrorCode("400", "第 " + (index + 1) + " 行项目名称不能为空"));
            }
            ContractChangeType rowType = item.getChangeType() == null ? batchType : item.getChangeType();
            ContractLine sourceLine = item.getSourceLineId() == null ? null : sourceLines.get(item.getSourceLineId());
            if (item.getSourceLineId() != null && sourceLine == null) {
                throw new DefinedException(new DefaultErrorCode("404", "第 " + (index + 1) + " 行原合同行不存在"));
            }
            // 金额服务端重算：替换/数量/价格必须关联原合同行，由服务端按原合同行计算，忽略前端金额；
            // 增项/减项才允许手填金额，且仍由服务端汇总，不信任前端总额。
            // 批量行实体字段默认 0，这里按变更类型规范化入参，避免 0 被误当成“显式零价/零量”。
            BigDecimal rowNewQuantity = item.getNewQuantity();
            BigDecimal rowNewPrice = item.getNewPrice();
            BigDecimal rowNewCost = item.getNewCost();
            if (sourceLine != null && rowType == ContractChangeType.Quantity
                    && (rowNewQuantity == null || rowNewQuantity.signum() <= 0)) {
                throw new DefinedException(new DefaultErrorCode("400", "第 " + (index + 1) + " 行数量变更必须填写大于 0 的新数量"));
            }
            if (sourceLine != null && rowType == ContractChangeType.Replace
                    && (rowNewQuantity == null || rowNewQuantity.signum() <= 0
                    || rowNewPrice == null || rowNewPrice.signum() <= 0)) {
                throw new DefinedException(new DefaultErrorCode("400", "第 " + (index + 1) + " 行替换必须填写新数量与新单价"));
            }
            if (sourceLine != null && rowType == ContractChangeType.Price
                    && (rowNewPrice == null || rowNewPrice.signum() <= 0)) {
                throw new DefinedException(new DefaultErrorCode("400", "第 " + (index + 1) + " 行价格变更必须填写大于 0 的新单价"));
            }
            if (rowType == ContractChangeType.Quantity) {
                rowNewPrice = null;
                rowNewCost = null;
            } else if (rowType == ContractChangeType.Price) {
                rowNewQuantity = null;
            }
            if ((rowType == ContractChangeType.Replace || rowType == ContractChangeType.Price)
                    && rowNewCost != null && rowNewCost.signum() <= 0) {
                rowNewCost = null; // 未填成本时按原合同行单位成本计算，避免默认 0 被当成零成本。
            }
            boolean recompute = sourceLine != null
                    && (rowType == ContractChangeType.Quantity
                    || rowType == ContractChangeType.Replace
                    || rowType == ContractChangeType.Price);
            ContractChangeAmountContext.Computed computed = amountContext.compute(
                    rowType, sourceLine,
                    recompute ? null : item.getAmountDelta(),
                    recompute ? null : item.getCostDelta(),
                    rowNewQuantity, rowNewPrice, rowNewCost);
            BigDecimal amountDelta = computed.amountDelta();
            BigDecimal costDelta = computed.costDelta();
            if (amountDelta.signum() == 0 && costDelta.signum() == 0) {
                throw new DefinedException(new DefaultErrorCode("400", "第 " + (index + 1) + " 行金额或成本不能同时为 0"));
            }
            if (rowType == ContractChangeType.Add && amountDelta.signum() < 0) {
                throw new DefinedException(new DefaultErrorCode("400", "增项金额不能为负数"));
            }
            if (rowType == ContractChangeType.Remove && amountDelta.signum() > 0) {
                throw new DefinedException(new DefaultErrorCode("400", "减项金额不能为正数"));
            }
            amount = amount.add(amountDelta);
            cost = cost.add(costDelta);

            item.setName(name);
            item.setChangeType(rowType);
            item.setSourceLine(sourceLine);
            if (item.getItemType() == null && sourceLine != null) item.setItemType(sourceLine.getType());
            item.setOldQuantity(computed.oldQuantity());
            item.setNewQuantity(computed.newQuantity());
            item.setOldPrice(computed.oldPrice());
            item.setNewPrice(computed.newPrice());
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
        // 新口径（DEC-041）：客户线下确认是创建前置事实；审批通过即生效。
        request.setCustomerConfirmed(true);
        request.setCustomerConfirmedAt(customerConfirmedAt);
        request.setCustomerConfirmedBy(customerConfirmedBy.trim());
        request.setCustomerConfirmMethod(customerConfirmMethod.trim());
        request.setRequestedBy(current);
        request.setEvidenceImages(evidenceImages);
        request.setSignatureUrl(signatureUrl);
        ContractChange saved = changeRepository.save(modelIdentityContext.prepare(request));

        // 效率：多行明细一次 saveAll，避免逐行 save 的 N 次往返。
        List<ContractChangeItem> toSave = new ArrayList<>(preparedItems.size());
        for (ContractChangeItem item : preparedItems) {
            item.setChange(saved);
            toSave.add(modelIdentityContext.prepare(item));
        }
        itemRepository.saveAll(toSave);
        saved.setItems(preparedItems);
        if (carryoverLockContext != null) carryoverLockContext.lock(project, CarryoverBusinessType.ContractChange, current);
        return saved;
    }

    /** DEC-041/BR-035：无客户线下确认时间、确认人、方式与签认证据，不得形成增减项变更单。 */
    private void requireCustomerSignOff() {
        if (customerConfirmedAt == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请登记客户线下确认时间"));
        }
        if (customerConfirmedBy == null || customerConfirmedBy.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请登记客户确认人"));
        }
        if (customerConfirmMethod == null || customerConfirmMethod.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择客户确认方式"));
        }
        boolean hasEvidence = (evidenceImages != null && !evidenceImages.isEmpty())
                || (signatureUrl != null && !signatureUrl.isBlank());
        if (!hasEvidence) {
            throw new DefinedException(new DefaultErrorCode("400", "请上传客户签认证据"));
        }
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
