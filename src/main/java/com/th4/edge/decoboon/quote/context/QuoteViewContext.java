package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 同一份报价面向客户和项目经理生成不同结构视图。 */
@Note("报价视图上下文")
@Component
public class QuoteViewContext {

    /** 客户视图按区域组织，项目经理视图按施工/材料分类组织。 */
    public Quote apply(Quote quote, List<QuoteLine> items) {
        List<QuoteLine> safeItems = items == null ? List.of() : items;
        quote.setItems(new ArrayList<>(safeItems));
        quote.setRegionGroups(groupRegion(safeItems));
        quote.setConstructionGroups(groupByCategory(safeItems, ItemType.Construction));
        quote.setMaterialGroups(groupByCategory(safeItems, ItemType.Material));
        quote.setOtherGroups(groupOther(safeItems));
        return quote;
    }

    /** 只使用项目经理结构，避免把区域层级暴露给履约端。 */
    public Quote applyManagerView(Quote quote, List<QuoteLine> items) {
        List<QuoteLine> safeItems = items == null ? List.of() : items;
        quote.setItems(null);
        quote.setRegionGroups(null);
        quote.setConstructionGroups(groupByCategory(safeItems, ItemType.Construction));
        quote.setMaterialGroups(groupByCategory(safeItems, ItemType.Material));
        quote.setOtherGroups(groupOther(safeItems));
        return quote;
    }

    private List<QuoteGroup> groupRegion(List<QuoteLine> items) {
        Map<String, QuoteGroup> groups = new LinkedHashMap<>();
        for (QuoteLine item : items) {
            Space space = item.getSpace();
            String packageSectionName = item.getPackageSectionNameSnapshot();
            String key;
            String name;
            if (packageSectionName != null && !packageSectionName.isBlank()) {
                name = packageSectionName.trim();
                key = "package-section-" + name.toLowerCase();
            } else {
                key = space == null || space.getSpaceId() == null
                        ? "general"
                        : "space-" + space.getSpaceId();
                name = space == null ? "综合项目" : space.getName();
            }
            String groupName = name;
            QuoteGroup group = groups.computeIfAbsent(key, ignored -> {
                QuoteGroup value = new QuoteGroup();
                value.setKey(key);
                value.setName(groupName);
                return value;
            });
            add(group, item);
        }
        return new ArrayList<>(groups.values());
    }

    private List<QuoteGroup> groupByCategory(List<QuoteLine> items, ItemType type) {
        Map<String, QuoteGroup> groups = new LinkedHashMap<>();
        for (QuoteLine item : items) {
            if (item.getType() != type) {
                continue;
            }
            String category = item.getCategory() == null || item.getCategory().isBlank()
                    ? type.getName()
                    : item.getCategory();
            String key = type.name() + "-" + category;
            QuoteGroup group = groups.computeIfAbsent(key, ignored -> {
                QuoteGroup value = new QuoteGroup();
                value.setKey(key);
                value.setName(category);
                value.setCategory(category);
                value.setType(type);
                return value;
            });
            add(group, item);
        }
        return new ArrayList<>(groups.values());
    }

    private List<QuoteGroup> groupOther(List<QuoteLine> items) {
        Map<String, QuoteGroup> groups = new LinkedHashMap<>();
        for (QuoteLine item : items) {
            if (item.getType() == ItemType.Construction || item.getType() == ItemType.Material) {
                continue;
            }
            ItemType type = item.getType() == null ? ItemType.General : item.getType();
            String category = item.getCategory() == null || item.getCategory().isBlank()
                    ? type.getName() : item.getCategory();
            String key = type.name() + "-" + category;
            QuoteGroup group = groups.computeIfAbsent(key, ignored -> {
                QuoteGroup value = new QuoteGroup();
                value.setKey(key);
                value.setName(category);
                value.setCategory(category);
                value.setType(type);
                return value;
            });
            add(group, item);
        }
        return new ArrayList<>(groups.values());
    }

    private void add(QuoteGroup group, QuoteLine item) {
        BigDecimal amount = effectiveAmount(item);
        BigDecimal cost = item.getCost() == null ? BigDecimal.ZERO : item.getCost();
        group.setAmount(group.getAmount().add(amount));
        group.setCost(group.getCost().add(cost));
        group.getItems().add(item);
    }

    private BigDecimal effectiveAmount(QuoteLine item) {
        if (item.getAmount() != null && item.getAmount().signum() != 0) {
            return item.getAmount();
        }
        BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
        BigDecimal quantity = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
        return price.multiply(quantity);
    }
}
