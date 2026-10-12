package com.th4.edge.decoboon.project.service.contractchange.batchsave.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.context.CarryoverLockContext;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.context.ContractChangeAmountContext;
import com.th4.edge.decoboon.project.context.ContractChangeRangeContext;
import com.th4.edge.decoboon.project.context.ContractChangeTargetContext;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 一次事务保存一张包含多行增减项的合同变更单。 */
@Note("批量发起合同变更")
@Description("新增分项/减项/替换逐条录入，一张单统一提交审批")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "no", "reason", "type",
                        "amount", "costDelta", "status", "customerConfirmed", "customerConfirmedAt",
                        "customerConfirmedBy", "customerConfirmMethod", "evidenceImages",
                        "signatureUrl", "items", "requestedBy", "createTime"}),
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

    /** 兼容旧请求的默认行类型；新页面每行显式传 changeType。 */
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
    private final ContractChangeRangeContext rangeContext;
    private final ContractChangeTargetContext targetContext;
    private final ProjectPermissionContext permissionContext;
    private final CarryoverLockContext carryoverLockContext;
    private final ModelIdentityContext modelIdentityContext;

    public BatchSave(ProjectContractRepository contractRepository,
                     ContractChangeRepository changeRepository,
                     ContractChangeItemRepository itemRepository,
                     ProjectAccessContext accessContext,
                     ContractChangeAmountContext amountContext,
                     ContractChangeRangeContext rangeContext,
                     ContractChangeTargetContext targetContext,
                     ProjectPermissionContext permissionContext,
                     ModelIdentityContext modelIdentityContext,
                     CarryoverLockContext carryoverLockContext) {
        this.contractRepository = contractRepository;
        this.changeRepository = changeRepository;
        this.itemRepository = itemRepository;
        this.accessContext = accessContext;
        this.amountContext = amountContext;
        this.rangeContext = rangeContext;
        this.targetContext = targetContext;
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

        ContractChange draft = prepareDraft(contract, project, type, items);
        request.setCompany(draft.getCompany());
        request.setContract(draft.getContract());
        request.setProject(draft.getProject());
        request.setProperty(draft.getProperty());
        permissionContext.apply(request, project);
        request.setNo("BC" + System.currentTimeMillis());
        request.setReason(reason.trim());
        request.setType(draft.getType());
        request.setAmount(draft.getAmount());
        request.setCostDelta(draft.getCostDelta());
        request.setStatus(draft.getStatus());
        // DEC-068：业主确认不建模为系统事实；建单只留痕发起人、时间与明细，审批通过即生效。
        request.setRequestedBy(current);
        ContractChange saved = changeRepository.save(modelIdentityContext.prepare(request));

        List<ContractChangeItem> toSave = new ArrayList<>(draft.getItems().size());
        for (ContractChangeItem item : draft.getItems()) {
            item.setChange(saved);
            toSave.add(modelIdentityContext.prepare(item));
        }
        itemRepository.saveAll(toSave);
        saved.setItems(draft.getItems());
        if (carryoverLockContext != null) carryoverLockContext.lock(project, CarryoverBusinessType.ContractChange, current);
        return saved;
    }


    /**
     * 执行新增/减项/替换的行级校验、目标快照与差额计算，但不写库。
     * batch-save 与 preview 共用本方法，禁止两条链路各自计算（预览即所得）。
     */
    public ContractChange prepareDraft(ProjectContract contract, Project project,
                                       ContractChangeType defaultType, List<ContractChangeItem> rows) {
        ContractChangeType batchType = defaultType == null ? ContractChangeType.Add : defaultType;
        List<Long> sourceLineIds = rows.stream()
                .map(ContractChangeItem::getSourceLineId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ContractLine> sourceLines = amountContext.lines(contract, sourceLineIds);
        Map<Long, ContractChangeItem> sourceChangeItems = loadSourceChangeItems(project.getCompany().getComId(), rows);

        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        boolean costComplete = true;
        Map<String, BigDecimal> batchReserved = new HashMap<>();
        List<ContractChangeItem> preparedItems = new ArrayList<>();

        for (int index = 0; index < rows.size(); index++) {
            ContractChangeItem item = rows.get(index);
            int rowNo = index + 1;
            ContractChangeType rowType = item.getChangeType() == null ? batchType : item.getChangeType();
            if (rowType == ContractChangeType.Price) {
                throw new DefinedException(new DefaultErrorCode("400", "新口径不再支持调价，请使用新增分项或替换"));
            }

            ContractLine sourceLine = item.getSourceLineId() == null ? null : sourceLines.get(item.getSourceLineId());
            if (item.getSourceLineId() != null && sourceLine == null) {
                throw new DefinedException(new DefaultErrorCode("404", "第 " + rowNo + " 行原合同行不存在"));
            }
            ContractChangeItem sourceChangeItem = item.getSourceChangeItemId() == null
                    ? null : sourceChangeItems.get(item.getSourceChangeItemId());
            if (item.getSourceChangeItemId() != null && sourceChangeItem == null) {
                throw new DefinedException(new DefaultErrorCode("404", "第 " + rowNo + " 行变更来源不存在"));
            }
            if (sourceLine != null && sourceChangeItem != null) {
                throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行来源只能选择一个"));
            }

            if (rowType == ContractChangeType.Add && item.getTargetCatalogItemId() != null) {
                prepareAdd(project, item, rowNo);
                amount = add(amount, item.getAmountDelta());
                cost = addCost(cost, item.getCostDelta());
                costComplete = costComplete && item.getCostDelta() != null;
            } else if (rowType == ContractChangeType.Quantity
                    && (item.getReduceQuantity() != null || sourceChangeItem != null)) {
                prepareReduce(project, contract, item, sourceLine, sourceChangeItem,
                        sourceKey(sourceLine, sourceChangeItem), batchReserved, rowNo);
                amount = add(amount, item.getAmountDelta());
                cost = addCost(cost, item.getCostDelta());
                costComplete = costComplete && item.getCostDelta() != null;
            } else if (rowType == ContractChangeType.Remove) {
                prepareRemove(project, contract, item, sourceLine, sourceChangeItem,
                        sourceKey(sourceLine, sourceChangeItem), batchReserved, rowNo);
                amount = add(amount, item.getAmountDelta());
                cost = addCost(cost, item.getCostDelta());
                costComplete = costComplete && item.getCostDelta() != null;
            } else if (rowType == ContractChangeType.Replace && item.getTargetCatalogItemId() != null) {
                prepareReplace(project, contract, item, sourceLine, sourceChangeItem,
                        sourceKey(sourceLine, sourceChangeItem), batchReserved, rowNo);
                amount = add(amount, item.getAmountDelta());
                cost = addCost(cost, item.getCostDelta());
                costComplete = costComplete && item.getCostDelta() != null;
            } else {
                // 兼容旧 Quantity/旧手动 Add 请求；新 App 不从这里创建。
                LegacyPrepared legacy = prepareLegacy(project, item, sourceLine, rowType, rowNo);
                amount = add(amount, legacy.amountDelta());
                cost = addCost(cost, legacy.costDelta());
                costComplete = costComplete && legacy.costDelta() != null;
            }

            item.setChangeType(rowType);
            item.setSourceLine(sourceLine);
            item.setSourceChangeItem(sourceChangeItem);
            permissionContext.apply(item, project);
            preparedItems.add(item);
        }

        ContractChange draft = new ContractChange();
        draft.setCompany(project.getCompany());
        draft.setContract(contract);
        draft.setProject(project);
        draft.setProperty(project.getProperty());
        permissionContext.apply(draft, project);
        draft.setType(batchType);
        draft.setAmount(amount);
        draft.setCostDelta(costComplete ? cost : null);
        draft.setStatus(ContractChangeStatus.Pending);
        draft.setItems(preparedItems);
        return draft;
    }

    private void prepareAdd(Project project, ContractChangeItem item, int rowNo) {
        BigDecimal quantity = firstPositive(item.getTargetQuantity(), item.getNewQuantity());
        ContractChangeTargetContext.TargetSnapshot target =
                targetContext.resolve(project.getCompany().getComId(), item.getTargetCatalogItemId(), quantity);
        targetContext.apply(item, target);
        item.setTargetCatalogItemId(target.catalog().getItemId());
        item.setName(target.catalog().getName());
        item.setItemType(target.catalog().getType());
        item.setSourceLine(null);
        item.setSourceChangeItem(null);
        item.setSourceRemainingQuantity(BigDecimal.ZERO);
        item.setSourceRemainingAmount(BigDecimal.ZERO);
        item.setSourceUnitPrice(BigDecimal.ZERO);
        item.setSourceBaselineTime(LocalDateTime.now());
        item.setOldQuantity(BigDecimal.ZERO);
        item.setNewQuantity(quantity);
        item.setOldPrice(BigDecimal.ZERO);
        item.setNewPrice(target.unitPrice());
        item.setAmountDelta(target.amount());
        item.setCostDelta(target.costAmount());
        requireNonZero(item, rowNo);
    }

    private void prepareReduce(Project project, ProjectContract contract, ContractChangeItem item,
                               ContractLine sourceLine, ContractChangeItem sourceChangeItem,
                               String sourceKey, Map<String, BigDecimal> batchReserved, int rowNo) {
        if (sourceKey == null) {
            throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行减项必须选择来源分项"));
        }
        ContractChangeRangeContext.RangeSnapshot snapshot = snapshot(contract, sourceLine, sourceChangeItem, rowNo);
        BigDecimal reduction = reductionQuantity(item, snapshot);
        BigDecimal reserved = batchReserved.getOrDefault(sourceKey, BigDecimal.ZERO);
        BigDecimal available = snapshot.availableQuantity().subtract(reserved);
        if (reduction.compareTo(available) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行减少工程量超过当前可变更剩余"));
        }
        BigDecimal remaining = snapshot.effectiveRemainingQuantity().subtract(reduction);
        BigDecimal sourceRemainingCost = sourceRemainingCost(sourceLine, sourceChangeItem, snapshot);
        BigDecimal costReduction = sourceRemainingCost == null
                ? null
                : snapshot.effectiveRemainingQuantity().signum() == 0
                ? BigDecimal.ZERO
                : money(sourceRemainingCost.multiply(reduction)
                .divide(snapshot.effectiveRemainingQuantity(), 6, RoundingMode.HALF_UP));
        setSourceSnapshot(item, snapshot, sourceLine, sourceChangeItem, sourceKey, batchReserved, reduction);
        item.setName(sourceName(sourceLine, sourceChangeItem));
        item.setItemType(sourceItemType(sourceLine, sourceChangeItem));
        item.setOldQuantity(snapshot.effectiveRemainingQuantity());
        item.setNewQuantity(remaining);
        item.setOldPrice(snapshot.currentUnitPrice());
        item.setNewPrice(snapshot.currentUnitPrice());
        item.setAmountDelta(money(reduction.multiply(snapshot.currentUnitPrice()).negate()));
        item.setCostDelta(costReduction == null ? null : costReduction.negate());
        requireNonZero(item, rowNo);
    }

    private void prepareRemove(Project project, ProjectContract contract, ContractChangeItem item,
                               ContractLine sourceLine, ContractChangeItem sourceChangeItem,
                               String sourceKey, Map<String, BigDecimal> batchReserved, int rowNo) {
        if (sourceKey == null) {
            throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行整项取消必须选择来源分项"));
        }
        if (batchReserved.getOrDefault(sourceKey, BigDecimal.ZERO).signum() > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行同一来源不能重复减项和整项取消"));
        }
        ContractChangeRangeContext.RangeSnapshot snapshot = snapshot(contract, sourceLine, sourceChangeItem, rowNo);
        BigDecimal sourceRemainingCost = sourceRemainingCost(sourceLine, sourceChangeItem, snapshot);
        setSourceSnapshot(item, snapshot, sourceLine, sourceChangeItem, sourceKey, batchReserved,
                snapshot.effectiveRemainingQuantity());
        item.setName(sourceName(sourceLine, sourceChangeItem));
        item.setItemType(sourceItemType(sourceLine, sourceChangeItem));
        item.setOldQuantity(snapshot.effectiveRemainingQuantity());
        item.setNewQuantity(BigDecimal.ZERO);
        item.setOldPrice(snapshot.currentUnitPrice());
        item.setNewPrice(snapshot.currentUnitPrice());
        item.setAmountDelta(snapshot.effectiveRemainingAmount().negate());
        item.setCostDelta(sourceRemainingCost == null ? null : sourceRemainingCost.negate());
        requireNonZero(item, rowNo);
    }

    private void prepareReplace(Project project, ProjectContract contract, ContractChangeItem item,
                                ContractLine sourceLine, ContractChangeItem sourceChangeItem,
                                String sourceKey, Map<String, BigDecimal> batchReserved, int rowNo) {
        if (sourceKey == null) {
            throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行替换必须选择来源分项"));
        }
        if (batchReserved.getOrDefault(sourceKey, BigDecimal.ZERO).signum() > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行同一来源不能重复替换"));
        }
        ContractChangeRangeContext.RangeSnapshot snapshot = snapshot(contract, sourceLine, sourceChangeItem, rowNo);
        BigDecimal quantity = firstPositive(item.getTargetQuantity(), item.getNewQuantity());
        ContractChangeTargetContext.TargetSnapshot target =
                targetContext.resolve(project.getCompany().getComId(), item.getTargetCatalogItemId(), quantity);
        BigDecimal sourceRemainingCost = sourceRemainingCost(sourceLine, sourceChangeItem, snapshot);
        setSourceSnapshot(item, snapshot, sourceLine, sourceChangeItem, sourceKey, batchReserved,
                snapshot.effectiveRemainingQuantity());
        targetContext.apply(item, target);
        item.setTargetCatalogItemId(target.catalog().getItemId());
        item.setName(target.catalog().getName());
        item.setItemType(target.catalog().getType());
        item.setOldQuantity(snapshot.effectiveRemainingQuantity());
        item.setNewQuantity(quantity);
        item.setOldPrice(snapshot.currentUnitPrice());
        item.setNewPrice(target.unitPrice());
        item.setAmountDelta(money(target.amount().subtract(snapshot.effectiveRemainingAmount())));
        item.setCostDelta(target.costAmount() == null || sourceRemainingCost == null
                ? null : money(target.costAmount().subtract(sourceRemainingCost)));
        requireNonZero(item, rowNo);
    }

    private LegacyPrepared prepareLegacy(Project project, ContractChangeItem item, ContractLine sourceLine,
                                         ContractChangeType rowType, int rowNo) {
        if (rowType == ContractChangeType.Replace && item.getTargetCatalogItemId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行替换必须选择报价项目库目标分项"));
        }
        boolean recompute = sourceLine != null
                && (rowType == ContractChangeType.Quantity || rowType == ContractChangeType.Price);
        BigDecimal legacyNewQuantity = rowType == ContractChangeType.Price ? null : item.getNewQuantity();
        BigDecimal legacyNewPrice = rowType == ContractChangeType.Quantity ? null : item.getNewPrice();
        ContractChangeAmountContext.Computed computed = amountContext.compute(
                rowType, sourceLine,
                recompute ? null : item.getAmountDelta(),
                recompute ? null : item.getCostDelta(),
                legacyNewQuantity, legacyNewPrice, item.getNewCost());
        item.setName(item.getName() == null ? "" : item.getName().trim());
        item.setSourceLine(sourceLine);
        item.setOldQuantity(computed.oldQuantity());
        item.setNewQuantity(computed.newQuantity());
        item.setOldPrice(computed.oldPrice());
        item.setNewPrice(computed.newPrice());
        item.setAmountDelta(computed.amountDelta());
        item.setCostDelta(computed.costDelta());
        if (item.getName().isBlank()) {
            item.setName(computed.oldQuantity().signum() == 0 ? "合同变更" : "合同变更调量");
        }
        if (item.getItemType() == null && sourceLine != null) item.setItemType(sourceLine.getType());
        requireNonZero(item, rowNo);
        return new LegacyPrepared(item.getAmountDelta(), item.getCostDelta());
    }

    private ContractChangeRangeContext.RangeSnapshot snapshot(ProjectContract contract, ContractLine sourceLine,
                                                               ContractChangeItem sourceChangeItem, int rowNo) {
        ContractChangeRangeContext.RangeSnapshot snapshot = sourceLine != null
                ? rangeContext.snapshot(contract, sourceLine)
                : rangeContext.snapshot(contract, sourceChangeItem);
        if (!snapshot.confirmable()) {
            throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行" + snapshot.blockReason()));
        }
        if (sourceLine == null && sourceChangeItem == null) {
            throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行来源不能为空"));
        }
        return snapshot;
    }

    private BigDecimal reductionQuantity(ContractChangeItem item,
                                         ContractChangeRangeContext.RangeSnapshot snapshot) {
        if (item.getReduceQuantity() != null) {
            if (item.getReduceQuantity().signum() <= 0) {
                throw new DefinedException(new DefaultErrorCode("400", "减少工程量必须大于 0"));
            }
            return item.getReduceQuantity();
        }
        if (item.getNewQuantity() == null || item.getNewQuantity().signum() < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "请填写减少工程量"));
        }
        BigDecimal reduction = snapshot.effectiveRemainingQuantity().subtract(item.getNewQuantity());
        if (reduction.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "减少工程量必须大于 0"));
        }
        return reduction;
    }

    private BigDecimal sourceRemainingCost(ContractLine sourceLine, ContractChangeItem sourceChangeItem,
                                           ContractChangeRangeContext.RangeSnapshot snapshot) {
        if (sourceLine != null) {
            return targetContext.sourceRemainingCost(
                    new ContractChangeTargetContext.ContractLineSnapshot(sourceLine.getQuantity(), sourceLine.getCost()),
                    snapshot.effectiveRemainingQuantity());
        }
        if (sourceChangeItem == null || sourceChangeItem.getTargetCostAmount() == null
                || sourceChangeItem.getTargetQuantity() == null) {
            return null;
        }
        return targetContext.sourceRemainingCost(
                new ContractChangeTargetContext.ContractLineSnapshot(sourceChangeItem.getTargetQuantity(),
                        sourceChangeItem.getTargetCostAmount()),
                snapshot.effectiveRemainingQuantity());
    }

    private void setSourceSnapshot(ContractChangeItem item,
                                   ContractChangeRangeContext.RangeSnapshot snapshot,
                                   ContractLine sourceLine, ContractChangeItem sourceChangeItem,
                                   String sourceKey, Map<String, BigDecimal> batchReserved,
                                   BigDecimal reservedQuantity) {
        item.setSourceLine(sourceLine);
        item.setSourceChangeItem(sourceChangeItem);
        item.setSourceLineId(sourceLine == null ? null : sourceLine.getLineId());
        item.setSourceChangeItemId(sourceChangeItem == null ? null : sourceChangeItem.getChangeItemId());
        item.setSourceRemainingQuantity(snapshot.effectiveRemainingQuantity());
        item.setSourceRemainingAmount(snapshot.effectiveRemainingAmount());
        item.setSourceUnitPrice(snapshot.currentUnitPrice());
        item.setSourceBaselineTime(LocalDateTime.now());
        batchReserved.merge(sourceKey, reservedQuantity, BigDecimal::add);
    }

    private String sourceKey(ContractLine sourceLine, ContractChangeItem sourceChangeItem) {
        if (sourceLine != null) return "line:" + sourceLine.getLineId();
        if (sourceChangeItem != null) return "item:" + sourceChangeItem.getChangeItemId();
        return null;
    }

    private String sourceName(ContractLine sourceLine, ContractChangeItem sourceChangeItem) {
        if (sourceLine != null) return sourceLine.getName();
        if (sourceChangeItem != null && sourceChangeItem.getTargetName() != null) return sourceChangeItem.getTargetName();
        if (sourceChangeItem != null) return sourceChangeItem.getName();
        return "合同变更";
    }

    private com.th4.edge.decoboon.catalog.configure.ItemType sourceItemType(ContractLine sourceLine,
                                                                            ContractChangeItem sourceChangeItem) {
        if (sourceLine != null) return sourceLine.getType();
        if (sourceChangeItem != null && sourceChangeItem.getTargetType() != null) {
            return sourceChangeItem.getTargetType();
        }
        return sourceChangeItem == null ? null : sourceChangeItem.getItemType();
    }

    private BigDecimal firstPositive(BigDecimal first, BigDecimal second) {
        BigDecimal value = first == null ? second : first;
        if (value == null || value.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "工程量必须大于 0"));
        }
        return value;
    }

    private void requireNonZero(ContractChangeItem item, int rowNo) {
        if (zero(item.getAmountDelta()).signum() == 0 && zero(item.getCostDelta()).signum() == 0) {
            throw new DefinedException(new DefaultErrorCode("400", "第 " + rowNo + " 行金额或成本不能同时为 0"));
        }
    }

    private BigDecimal add(BigDecimal left, BigDecimal right) {
        return left.add(zero(right));
    }

    private BigDecimal addCost(BigDecimal left, BigDecimal right) {
        return right == null ? left : left.add(right);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, ContractChangeItem> loadSourceChangeItems(Long companyId, List<ContractChangeItem> rows) {
        Map<Long, ContractChangeItem> result = new HashMap<>();
        for (ContractChangeItem row : rows) {
            if (row.getSourceChangeItemId() == null || result.containsKey(row.getSourceChangeItemId())) continue;
            ContractChangeItem source = itemRepository
                    .findByChangeItemIdAndCompany_ComId(row.getSourceChangeItemId(), companyId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "变更目标来源不存在或无权访问")));
            result.put(row.getSourceChangeItemId(), source);
        }
        return result;
    }

    // DEC-068：删除客户线下确认门禁（原 requireCustomerSignOff）。
    // 老版本客户端可能仍提交 customerConfirmedAt/By/Method、signatureUrl、evidenceImages，服务端忽略不校验。

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record LegacyPrepared(BigDecimal amountDelta, BigDecimal costDelta) {
    }
}
