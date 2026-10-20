package com.th4.edge.decoboon.design.service.items.reorder.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设计分项排序实现。
 *
 * <p>整单提交当前可见全部分项的 itemId，按数组顺序写回连续 sort。
 * 该入口只改变展示顺序，不重新读取/校验报价项目库，也不重算金额与成本，
 * 避免历史停用项目或旧公式在纯排序时被隐式覆盖。</p>
 */
@Note("设计分项排序")
@Properties(allow = {
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "status", "version", "area", "layout", "style",
                        "spaces", "items", "updateTime"}),
        @AllowProperties(model = DesignItem.class,
                properties = {"itemId", "space", "type", "category", "catalogItem", "name", "brand", "spec", "unit",
                        "quantity", "quantitySource", "materialPrice", "manualPrice", "fixedTotalPrice", "costPrice", "pricingMode",
                        "amount", "cost", "formula", "quantityFormula", "pendingMeasure", "criterion", "craft", "remark", "sort", "images"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "sourceMeasureRegionId", "sort", "remark"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "materialPrice", "manualPrice", "fixedTotalPrice", "costPrice", "pricingMode",
                        "formula", "criterion", "craft", "active"})
})
@Service("design/items/reorder")
@Scope(ScopeTypes.PROTOTYPE)
public class Reorder implements com.th4.edge.decoboon.design.service.items.reorder.Reorder {

    @AutoInject
    @NotNull
    private Long designId;

    @AutoInject
    @NotNull
    private List<Long> itemIds;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final DesignItemRepository itemRepository;
    private final SpaceRepository spaceRepository;

    public Reorder(DesignContext designContext,
                   DesignItemRepository itemRepository,
                   SpaceRepository spaceRepository) {
        this.designContext = designContext;
        this.itemRepository = itemRepository;
        this.spaceRepository = spaceRepository;
    }

    @Override
    @Transactional
    public Design call(Design ignored) {
        Design design = designContext.requireDesign(designId, employee);
        designContext.requireEditable(design);

        List<DesignItem> current = itemRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());
        List<DesignItem> ordered = requireCompleteOrder(current);

        for (int index = 0; index < ordered.size(); index++) {
            ordered.get(index).setSort(index);
        }
        itemRepository.saveAll(ordered);

        design.setSpaces(spaceRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId()));
        design.setItems(ordered);
        return design;
    }

    private List<DesignItem> requireCompleteOrder(List<DesignItem> current) {
        if (itemIds == null || itemIds.size() != current.size()) {
            throw invalidOrder();
        }
        Map<Long, DesignItem> byId = new HashMap<>();
        for (DesignItem item : current) {
            byId.put(item.getItemId(), item);
        }
        Set<Long> requested = new HashSet<>();
        List<DesignItem> ordered = new ArrayList<>(current.size());
        for (Long itemId : itemIds) {
            if (itemId == null || !requested.add(itemId)) {
                throw invalidOrder();
            }
            DesignItem item = byId.get(itemId);
            if (item == null) {
                throw invalidOrder();
            }
            ordered.add(item);
        }
        return ordered;
    }

    private DefinedException invalidOrder() {
        return new DefinedException(new DefaultErrorCode("400",
                "分项排序数据不完整或不属于当前方案，请刷新后重试"));
    }
}
