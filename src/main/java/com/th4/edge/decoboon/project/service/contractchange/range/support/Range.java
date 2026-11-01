package com.th4.edge.decoboon.project.service.contractchange.range.support;

import com.th4.edge.decoboon.catalog.configure.ItemType;
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
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 增减项可变更来源与当前有效剩余投影（DEC-055 / BR-041 / DEC-068）。
 *
 * <p>用途：App / Web 在「减项」「替换」时展示「当前有效剩余 · 待审批占用 · 可变更量」，
 * 并在录入阶段做超减预校验。金额与可变更量一律来自 {@link ContractChangeRangeContext} 服务端投影，
 * 前端展示值不作为提交依据：正式提交由 batch-save / preview 重新投影与校验。</p>
 *
 * <p>选择器来源按 PICKER_PAGE 契约返回：原合同行走数据库 count/page，已生效变更目标使用同一次
 * 变更明细加载结果；当前有效剩余所需的历史明细只加载一次，禁止每个来源重复查库。</p>
 */
@Note("增减项可变更来源与当前有效剩余")
@Properties(allow = {
        @AllowProperties(model = ContractChangeRange.class,
                properties = {"contract", "current", "size", "total", "hasMore", "contractTotal", "effectiveTotal",
                        "itemTypes", "spaces", "sources", "selected", "confirmable"}),
        @AllowProperties(model = ContractChangeRange.Facet.class,
                properties = {"key", "label", "count"}),
        @AllowProperties(model = ContractChangeRange.Source.class,
                properties = {"group", "sourceLineId", "sourceChangeItemId", "changeId", "changeNo",
                        "name", "itemType", "brand", "spec", "spaceName", "unit", "unitKey",
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

    private static final String NONE_SPACE = "__none__";
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final Sort LINE_SORT = Sort.by(Sort.Order.asc("sort"), Sort.Order.asc("lineId"));

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

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    /** 服务端关键词搜索：名称/品牌/规格/区域/变更单号。 */
    @AutoInject
    private String keyword;

    /** 服务端分项类型筛选：ItemType 枚举名。 */
    @AutoInject
    private String itemType;

    /** 服务端区域筛选；__none__ 表示综合项目。 */
    @AutoInject
    private String spaceName;

    /** 服务端来源分组筛选：contract / effective；空表示全部。 */
    @AutoInject
    private String group;

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
        Employee currentEmployee = accessContext.requireEmployee(employee);
        Specification<ProjectContract> byId = (root, query, builder) ->
                builder.equal(root.get("contractId"), contractId);
        ProjectContract contract = contractRepository.findOne(byId.and(accessContext.visibleContract(employee)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合同基线不存在或无权访问")));
        if (contract.getProject() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "合同尚未生成项目"));
        }
        accessContext.requireProject(contract.getProject().getProjectId(), currentEmployee);

        List<ContractChangeItem> changeItems = itemRepository
                .findByChange_Contract_ContractIdOrderByChangeItemIdAsc(contractId);
        List<ContractChangeItem> effectiveItems = changeItems.stream()
                .filter(item -> item.getChangeItemId() != null
                        && item.getChange() != null
                        && item.getChange().getStatus() == ContractChangeStatus.CustomerConfirmed)
                .toList();

        int page = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String normalizedGroup = normalizeGroup(group);
        ItemType typeFilter = parseItemType(itemType);
        String spaceFilter = normalizeSpaceFilter(spaceName);

        Specification<ContractLine> lineSpec = lineSpecification(contract, keyword, typeFilter, spaceFilter);
        long contractTotal = lineRepository.count(lineSpec);
        List<ContractChangeItem> effectiveFiltered = effectiveItems.stream()
                .filter(item -> matchesEffective(item, keyword, typeFilter, spaceFilter))
                .toList();
        long effectiveTotal = effectiveFiltered.size();
        long filteredContractTotal = "effective".equals(normalizedGroup) ? 0L : contractTotal;
        long filteredEffectiveTotal = "contract".equals(normalizedGroup) ? 0L : effectiveTotal;
        long total = filteredContractTotal + filteredEffectiveTotal;
        long offset = (long) (page - 1) * pageSize;

        List<ContractChangeRange.Source> sources = new ArrayList<>();
        if (offset < filteredContractTotal) {
            int lineLimit = (int) Math.min(pageSize, filteredContractTotal - offset);
            // PageRequest 的 pageNumber 从 0 开始，不是全局元素 offset。
            List<ContractLine> lines = lineRepository.findAll(
                    lineSpec, PageRequest.of(page - 1, pageSize, LINE_SORT)).getContent();
            for (ContractLine line : lines.subList(0, Math.min(lineLimit, lines.size()))) {
                sources.add(source(contract, line, null, changeItems));
            }
        }
        int remaining = pageSize - sources.size();
        if (remaining > 0 && filteredEffectiveTotal > 0) {
            long effectiveAbsoluteOffset = Math.max(0L, offset - filteredContractTotal);
            if (effectiveAbsoluteOffset < filteredEffectiveTotal) {
                int from = (int) effectiveAbsoluteOffset;
                int to = (int) Math.min(effectiveFiltered.size(), effectiveAbsoluteOffset + remaining);
                for (ContractChangeItem item : effectiveFiltered.subList(from, to)) {
                    sources.add(source(contract, null, item, changeItems));
                }
            }
        }

        ContractChangeRange view = new ContractChangeRange();
        view.setContract(contract);
        view.setCurrent(page);
        view.setSize(pageSize);
        view.setTotal(total);
        view.setHasMore(offset + sources.size() < total);
        view.setContractTotal(contractTotal);
        view.setEffectiveTotal(effectiveTotal);
        applyFacets(view, contract, effectiveItems);
        view.setSources(sources);
        view.setSelected(selectedSource(contract, changeItems));
        view.setConfirmable(sources.stream().allMatch(item -> Boolean.TRUE.equals(item.getConfirmable())));
        return view;
    }

    private ContractChangeRange.Source selectedSource(ProjectContract contract,
                                                       List<ContractChangeItem> changeItems) {
        if (sourceLineId != null) {
            ContractLine line = lineRepository.findByLineIdAndContract_ContractId(sourceLineId, contractId)
                    .orElse(null);
            return line == null ? null : source(contract, line, null, changeItems);
        }
        if (sourceChangeItemId != null) {
            return changeItems.stream()
                    .filter(item -> sourceChangeItemId.equals(item.getChangeItemId()))
                    .filter(item -> item.getChange() != null
                            && item.getChange().getStatus() == ContractChangeStatus.CustomerConfirmed)
                    .findFirst()
                    .map(item -> source(contract, null, item, changeItems))
                    .orElse(null);
        }
        return null;
    }

    private Specification<ContractLine> lineSpecification(ProjectContract contract,
                                                           String keyword,
                                                           ItemType typeFilter,
                                                           String spaceFilter) {
        String search = normalizeKeyword(keyword);
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("contract"), contract));
            if (typeFilter != null) {
                predicates.add(builder.equal(root.get("type"), typeFilter));
            }
            if (spaceFilter != null) {
                if (NONE_SPACE.equals(spaceFilter)) {
                    predicates.add(builder.or(builder.isNull(root.get("spaceName")),
                            builder.equal(root.get("spaceName"), "")));
                } else {
                    predicates.add(builder.equal(root.get("spaceName"), spaceFilter));
                }
            }
            if (!search.isEmpty()) {
                String like = "%" + search + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(builder.coalesce(root.get("name"), "")), like),
                        builder.like(builder.lower(builder.coalesce(root.get("brand"), "")), like),
                        builder.like(builder.lower(builder.coalesce(root.get("spec"), "")), like),
                        builder.like(builder.lower(builder.coalesce(root.get("spaceName"), "")), like)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private boolean matchesEffective(ContractChangeItem item, String keyword,
                                     ItemType typeFilter, String spaceFilter) {
        String search = normalizeKeyword(keyword);
        ItemType itemType = item.getTargetType() != null ? item.getTargetType() : item.getItemType();
        String itemSpace = item.getSourceLine() == null ? null : item.getSourceLine().getSpaceName();
        if (typeFilter != null && itemType != typeFilter) {
            return false;
        }
        String normalizedSpace = normalizeSpaceFilter(spaceFilter);
        if (normalizedSpace != null) {
            String actualSpace = normalizeSpaceFilter(itemSpace);
            if (!normalizedSpace.equals(actualSpace == null ? NONE_SPACE : actualSpace)) {
                return false;
            }
        }
        if (search.isEmpty()) {
            return true;
        }
        String changeNo = item.getChange() == null ? null : item.getChange().getNo();
        String name = item.getTargetName() != null ? item.getTargetName() : item.getName();
        return contains(name, search)
                || contains(item.getTargetBrand(), search)
                || contains(item.getTargetSpec(), search)
                || contains(itemSpace, search)
                || contains(changeNo, search);
    }

    private void applyFacets(ContractChangeRange view, ProjectContract contract,
                             List<ContractChangeItem> effectiveItems) {
        Map<String, ContractChangeRange.Facet> itemTypes = new LinkedHashMap<>();
        Map<String, ContractChangeRange.Facet> spaces = new LinkedHashMap<>();
        for (Object[] row : lineRepository.summarizePickerFacetsByContract(contract)) {
            ItemType type = row[0] == null ? null : (ItemType) row[0];
            String space = row[1] == null ? NONE_SPACE : String.valueOf(row[1]);
            long count = row[2] == null ? 0L : ((Number) row[2]).longValue();
            typeFacet(itemTypes, type).setCount(typeFacet(itemTypes, type).getCount() + count);
            spaceFacet(spaces, space).setCount(spaceFacet(spaces, space).getCount() + count);
        }
        for (ContractChangeItem item : effectiveItems) {
            ItemType type = item.getTargetType() != null ? item.getTargetType() : item.getItemType();
            String space = item.getSourceLine() == null || item.getSourceLine().getSpaceName() == null
                    || item.getSourceLine().getSpaceName().isBlank()
                    ? NONE_SPACE : item.getSourceLine().getSpaceName();
            typeFacet(itemTypes, type).setCount(typeFacet(itemTypes, type).getCount() + 1);
            spaceFacet(spaces, space).setCount(spaceFacet(spaces, space).getCount() + 1);
        }
        view.setItemTypes(new ArrayList<>(itemTypes.values()));
        view.setSpaces(new ArrayList<>(spaces.values()));
    }

    private ContractChangeRange.Facet typeFacet(Map<String, ContractChangeRange.Facet> facets, ItemType type) {
        String key = type == null ? NONE_SPACE : type.name();
        return facets.computeIfAbsent(key,
                ignored -> new ContractChangeRange.Facet(key, type == null ? "未分类" : type.getName(), 0L));
    }

    private ContractChangeRange.Facet spaceFacet(Map<String, ContractChangeRange.Facet> facets, String space) {
        String key = space == null || space.isBlank() ? NONE_SPACE : space;
        return facets.computeIfAbsent(key,
                ignored -> new ContractChangeRange.Facet(key, NONE_SPACE.equals(key) ? "综合项目" : key, 0L));
    }

    private ContractChangeRange.Source source(ProjectContract contract, ContractLine line,
                                              ContractChangeItem changeItem,
                                              List<ContractChangeItem> changeItems) {
        ContractChangeRangeContext.RangeSnapshot snapshot = changeItem == null
                ? rangeContext.snapshot(contract, line, changeItems)
                : rangeContext.snapshot(contract, changeItem, changeItems);

        ContractChangeRange.Source source = new ContractChangeRange.Source();
        if (line != null) {
            source.setGroup("contract");
            source.setSourceLineId(line.getLineId());
            source.setName(line.getName());
            source.setItemType(line.getType() == null ? null : line.getType().name());
            source.setBrand(line.getBrand());
            source.setSpec(line.getSpec());
            source.setSpaceName(line.getSpaceName());
            source.setUnit(line.getUnit() == null ? null : line.getUnit().getName());
            source.setUnitKey(line.getUnit() == null ? null : line.getUnit().name());
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
            source.setSpaceName(changeItem.getSourceLine() == null ? null : changeItem.getSourceLine().getSpaceName());
            if (changeItem.getTargetUnit() != null) {
                source.setUnit(changeItem.getTargetUnit().getName());
                source.setUnitKey(changeItem.getTargetUnit().name());
            } else if (changeItem.getSourceLine() != null && changeItem.getSourceLine().getUnit() != null) {
                source.setUnit(changeItem.getSourceLine().getUnit().getName());
                source.setUnitKey(changeItem.getSourceLine().getUnit().name());
            }
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

    private ItemType parseItemType(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return ItemType.valueOf(value);
        } catch (IllegalArgumentException error) {
            throw new DefinedException(new DefaultErrorCode("400", "分项类型不合法"));
        }
    }

    private String normalizeGroup(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return "all";
        }
        if ("contract".equalsIgnoreCase(value) || "effective".equalsIgnoreCase(value)) {
            return value.toLowerCase(Locale.ROOT);
        }
        throw new DefinedException(new DefaultErrorCode("400", "来源分组不合法"));
    }

    private String normalizeSpaceFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private String normalizeKeyword(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    /** 供研发核对：可变更量为空时按 0 处理，避免前端出现 null 与 0 的两种空语义。 */
    static BigDecimal availableOrZero(ContractChangeRange.Source source) {
        return source.getAvailableQuantity() == null ? BigDecimal.ZERO : source.getAvailableQuantity();
    }
}
