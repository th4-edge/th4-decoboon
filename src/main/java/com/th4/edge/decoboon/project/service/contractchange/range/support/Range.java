package com.th4.edge.decoboon.project.service.contractchange.range.support;

import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.context.ContractChangeRangeContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractChangeRange;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 增减项可变更来源与当前有效剩余投影（DEC-055 / BR-041 / DEC-068）。
 *
 * <p>用途：App / Web 在「减项」「替换」时展示「当前有效剩余 · 待审批占用 · 可变更量」，
 * 并在录入阶段做超减预校验。金额与可变更量一律来自 {@link ContractChangeRangeContext} 服务端投影，
 * 前端展示值不作为提交依据：正式提交由 batch-save / preview 重新投影与校验。</p>
 */
@Note("增减项可变更来源与当前有效剩余")
@Properties(allow = {
        @AllowProperties(model = ContractChangeRange.class,
                properties = {"contract", "sources", "selected", "confirmable"}),
        @AllowProperties(model = ContractChangeRange.Source.class,
                properties = {"group", "sourceLineId", "sourceChangeItemId", "changeId", "changeNo",
                        "name", "itemType", "brand", "spec", "unit",
                        "initialQuantity", "effectiveRemainingQuantity", "pendingReservedQuantity",
                        "availableQuantity", "effectiveRemainingAmount", "currentUnitPrice",
                        "confirmable", "blockReason"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "signedAt"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract-change/range")
@Scope(ScopeTypes.PROTOTYPE)
public class Range implements com.th4.edge.decoboon.project.service.contractchange.range.Range {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long contractId;

    /** 可选：指定原合同行作为来源，返回 selected 便于前端直接展示。 */
    @AutoInject
    private Long sourceLineId;

    /** 可选：指定已生效变更目标作为来源，返回 selected 便于前端直接展示。 */
    @AutoInject
    private Long sourceChangeItemId;

    private final ProjectContractRepository contractRepository;
    private final ContractLineRepository lineRepository;
    private final ContractChangeItemRepository itemRepository;
    private final ContractChangeRangeContext rangeContext;
    private final ProjectAccessContext accessContext;

    public Range(ProjectContractRepository contractRepository,
                 ContractLineRepository lineRepository,
                 ContractChangeItemRepository itemRepository,
                 ContractChangeRangeContext rangeContext,
                 ProjectAccessContext accessContext) {
        this.contractRepository = contractRepository;
        this.lineRepository = lineRepository;
        this.itemRepository = itemRepository;
        this.rangeContext = rangeContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public ContractChangeRange call(ContractChange request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<ProjectContract> byId = (root, query, builder) ->
                builder.equal(root.get("contractId"), contractId);
        ProjectContract contract = contractRepository.findOne(byId.and(accessContext.visibleContract(employee)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合同基线不存在或无权访问")));
        if (contract.getProject() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "合同尚未生成项目"));
        }
        accessContext.requireProject(contract.getProject().getProjectId(), current);

        List<ContractChangeRange.Source> sources = new ArrayList<>();
        for (ContractLine line : lineRepository.findByContractOrderBySortAsc(contract)) {
            sources.add(source(contract, line, null));
        }
        for (ContractChangeItem item : itemRepository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(contractId)) {
            if (item.getChangeItemId() == null) continue;
            if (item.getChange() == null || item.getChange().getStatus() != ContractChangeStatus.CustomerConfirmed) {
                continue; // 只有已生效变更的目标才构成新的可变更范围（DEC-068：CustomerConfirmed = 已生效）
            }
            sources.add(source(contract, null, item));
        }

        ContractChangeRange view = new ContractChangeRange();
        view.setContract(contract);
        view.setSources(sources);
        view.setSelected(select(sources));
        view.setConfirmable(sources.stream().allMatch(item -> Boolean.TRUE.equals(item.getConfirmable())));
        return view;
    }

    private ContractChangeRange.Source select(List<ContractChangeRange.Source> sources) {
        if (sourceLineId != null) {
            return sources.stream()
                    .filter(item -> sourceLineId.equals(item.getSourceLineId()))
                    .findFirst().orElse(null);
        }
        if (sourceChangeItemId != null) {
            return sources.stream()
                    .filter(item -> sourceChangeItemId.equals(item.getSourceChangeItemId()))
                    .findFirst().orElse(null);
        }
        return null;
    }

    private ContractChangeRange.Source source(ProjectContract contract, ContractLine line, ContractChangeItem changeItem) {
        ContractChangeRangeContext.RangeSnapshot snapshot = changeItem == null
                ? rangeContext.snapshot(contract, line)
                : rangeContext.snapshot(contract, changeItem);

        ContractChangeRange.Source source = new ContractChangeRange.Source();
        if (line != null) {
            source.setGroup("contract");
            source.setSourceLineId(line.getLineId());
            source.setName(line.getName());
            source.setItemType(line.getType() == null ? null : line.getType().name());
            source.setBrand(line.getBrand());
            source.setSpec(line.getSpec());
            source.setUnit(line.getUnit() == null ? null : line.getUnit().getName());
        } else {
            source.setGroup("effective");
            source.setSourceChangeItemId(changeItem.getChangeItemId());
            source.setChangeId(changeItem.getChange() == null ? null : changeItem.getChange().getChangeId());
            source.setChangeNo(changeItem.getChange() == null ? null : changeItem.getChange().getNo());
            source.setName(changeItem.getTargetName() != null ? changeItem.getTargetName() : changeItem.getName());
            source.setItemType(changeItem.getTargetType() != null
                    ? changeItem.getTargetType().name()
                    : (changeItem.getItemType() == null ? null : changeItem.getItemType().name()));
            source.setBrand(changeItem.getTargetBrand());
            source.setSpec(changeItem.getTargetSpec());
            source.setUnit(changeItem.getTargetUnit() != null
                    ? changeItem.getTargetUnit().getName()
                    : source.getUnit());
        }
        source.setInitialQuantity(snapshot.initialQuantity());
        source.setEffectiveRemainingQuantity(snapshot.effectiveRemainingQuantity());
        source.setPendingReservedQuantity(snapshot.pendingReservedQuantity());
        source.setAvailableQuantity(snapshot.availableQuantity());
        source.setEffectiveRemainingAmount(snapshot.effectiveRemainingAmount());
        source.setCurrentUnitPrice(snapshot.currentUnitPrice());
        source.setConfirmable(snapshot.confirmable());
        source.setBlockReason(snapshot.blockReason());
        return source;
    }

    /** 供研发核对：可变更量为空时按 0 处理，避免前端出现 null 与 0 的两种空语义。 */
    static BigDecimal availableOrZero(ContractChangeRange.Source source) {
        return source.getAvailableQuantity() == null ? BigDecimal.ZERO : source.getAvailableQuantity();
    }
}
