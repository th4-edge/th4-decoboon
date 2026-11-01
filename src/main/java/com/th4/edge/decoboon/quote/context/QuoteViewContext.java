package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.configure.QuoteViewMode;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.catalog.support.CatalogQuoteSourceSupport;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.model.QuoteViewContract;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** 同一份报价的服务端区域/类型统一投影。 */
@Note("报价视图上下文")
@Component
public class QuoteViewContext {

    private static final String NO_REGION = "综合项目";
    private static final Comparator<QuoteLine> REGION_LINE_ORDER = Comparator
            .comparingInt(QuoteViewContext::regionSort)
            .thenComparing(line -> regionName(line.getSpace()))
            .thenComparing(QuoteViewContext::spaceId, Comparator.nullsLast(Long::compareTo))
            .thenComparingInt(QuoteViewContext::lineSort)
            .thenComparing(QuoteLine::getItemId, Comparator.nullsLast(Long::compareTo));
    private static final Comparator<QuoteLine> TYPE_LINE_ORDER = Comparator
            .comparingInt((QuoteLine line) -> typeOrder(effectiveType(line)))
            .thenComparing(QuoteViewContract::normalizedCategory)
            .thenComparing(line -> normalizedText(line.getName()))
            .thenComparing(line -> normalizedText(line.getBrand()))
            .thenComparing(line -> normalizedText(line.getSpec()))
            .thenComparing(QuoteLine::getItemId, Comparator.nullsLast(Long::compareTo));

    /** 返回同一原始行集合的区域、类型和兼容分类投影。 */
    public Quote apply(Quote quote, List<QuoteLine> items) {
        return project(quote, items, true, true);
    }

    /** 客户打印投影：使用同一分组规则，但不读取成本字段。 */
    public Quote applyForPrint(Quote quote, List<QuoteLine> items) {
        return project(quote, items, true, false);
    }

    /** 项目履约场景不返回原始行，但必须保留区域和类型事实。 */
    public Quote applyManagerView(Quote quote, List<QuoteLine> items) {
        return project(quote, items, false, true);
    }

    private Quote project(Quote quote, List<QuoteLine> items, boolean includeRawItems, boolean includeCost) {
        List<QuoteLine> safeItems = items == null ? List.of() : items;
        quote.setItems(includeRawItems ? new ArrayList<>(safeItems) : null);
        quote.setRegionGroups(groupRegion(safeItems, includeCost));
        quote.setTypeGroups(groupType(quote, safeItems, includeCost));
        quote.setConstructionGroups(groupByCategory(safeItems, ItemType.Construction, includeCost));
        quote.setMaterialGroups(groupByCategory(safeItems, ItemType.Material, includeCost));
        quote.setOtherGroups(groupOther(safeItems, includeCost));
        return quote;
    }

    private List<QuoteGroup> groupRegion(List<QuoteLine> items, boolean includeCost) {
        Map<String, QuoteGroup> groups = new LinkedHashMap<>();
        for (QuoteLine item : items.stream().sorted(REGION_LINE_ORDER).toList()) {
            Space space = item.getSpace();
            String packageSectionName = item.getPackageSectionNameSnapshot();
            String key;
            String name;
            if (packageSectionName != null && !packageSectionName.isBlank()) {
                name = packageSectionName.trim();
                key = "package-section-" + name.toLowerCase();
            } else {
                key = regionKey(space);
                name = regionName(space);
            }
            String groupName = name;
            QuoteGroup group = groups.computeIfAbsent(key, ignored -> {
                QuoteGroup value = new QuoteGroup();
                value.setKey(key);
                value.setName(groupName);
                value.setGroupMode(QuoteViewMode.REGION);
                return value;
            });
            add(group, item, includeCost);
        }
        return new ArrayList<>(groups.values());
    }

    private List<QuoteGroup> groupType(Quote quote, List<QuoteLine> items, boolean includeCost) {
        Map<String, TypeGroupAccumulator> groups = new LinkedHashMap<>();
        for (QuoteLine item : items.stream().sorted(TYPE_LINE_ORDER).toList()) {
            ItemType type = effectiveType(item);
            String category = displayCategory(item, type);
            String groupKey = type.name() + "|" + QuoteViewContract.normalizedCategory(item);
            groups.computeIfAbsent(groupKey, ignored -> new TypeGroupAccumulator(groupKey, type, category, includeCost))
                    .merge(QuoteViewContract.canonicalMergeKey(item), item);
        }
        return groups.values().stream()
                .sorted(Comparator
                        .comparingInt((TypeGroupAccumulator group) -> typeOrder(group.type))
                        .thenComparing(group -> normalizedText(group.category))
                        .thenComparing(group -> group.key))
                .map(group -> group.toGroup(quote))
                .toList();
    }

    private List<QuoteGroup> groupByCategory(List<QuoteLine> items, ItemType type, boolean includeCost) {
        Map<String, QuoteGroup> groups = new LinkedHashMap<>();
        for (QuoteLine item : items.stream().sorted(TYPE_LINE_ORDER).toList()) {
            if (item.getType() != type) {
                continue;
            }
            String category = displayCategory(item, type);
            String key = type.name() + "-" + category;
            QuoteGroup group = groups.computeIfAbsent(key, ignored -> {
                QuoteGroup value = new QuoteGroup();
                value.setKey(key);
                value.setName(category);
                value.setCategory(category);
                value.setType(type);
                return value;
            });
            add(group, item, includeCost);
        }
        return new ArrayList<>(groups.values());
    }

    private List<QuoteGroup> groupOther(List<QuoteLine> items, boolean includeCost) {
        Map<String, QuoteGroup> groups = new LinkedHashMap<>();
        for (QuoteLine item : items.stream().sorted(TYPE_LINE_ORDER).toList()) {
            if (item.getType() == ItemType.Construction || item.getType() == ItemType.Material) {
                continue;
            }
            ItemType type = effectiveType(item);
            String category = displayCategory(item, type);
            String key = type.name() + "-" + category;
            QuoteGroup group = groups.computeIfAbsent(key, ignored -> {
                QuoteGroup value = new QuoteGroup();
                value.setKey(key);
                value.setName(category);
                value.setCategory(category);
                value.setType(type);
                return value;
            });
            add(group, item, includeCost);
        }
        return new ArrayList<>(groups.values());
    }

    private void add(QuoteGroup group, QuoteLine item, boolean includeCost) {
        group.setQuantity(group.getQuantity().add(value(item.getQuantity())));
        group.setAmount(sumNullable(group.getAmount(), item.getAmount()));
        group.setCost(includeCost ? sumNullable(group.getCost(), item.getCost()) : null);
        group.getItems().add(item);
    }

    private static QuoteLine copySnapshot(QuoteLine source) {
        QuoteLine target = new QuoteLine();
        target.setType(source.getType());
        target.setCategory(source.getCategory());
        target.setName(source.getName());
        target.setBrand(source.getBrand());
        target.setSpec(source.getSpec());
        target.setUnit(source.getUnit());
        target.setMaterialPrice(source.getMaterialPrice());
        target.setManualPrice(source.getManualPrice());
        target.setFixedTotalPrice(source.getFixedTotalPrice());
        target.setPricingMode(source.getPricingMode());
        target.setPrice(source.getPrice());
        target.setFormula(source.getFormula());
        target.setQuantityFormula(source.getQuantityFormula());
        target.setRole(source.getRole());
        target.setDifferenceType(source.getDifferenceType());
        target.setDifferenceStatus(source.getDifferenceStatus());
        target.setIncluded(source.getIncluded());
        target.setDiscountAmount(source.getDiscountAmount());
        target.setCraft(source.getCraft());
        target.setSort(source.getSort());
        target.setCatalogItem(source.getCatalogItem());
        CatalogQuoteSourceSupport.copy(source, target);
        target.setSpace(null);
        target.setQuantity(BigDecimal.ZERO);
        target.setAmount(BigDecimal.ZERO);
        target.setCost(BigDecimal.ZERO);
        return target;
    }

    private static int regionSort(QuoteLine line) {
        Space space = line.getSpace();
        return space == null || space.getSort() == null ? Integer.MAX_VALUE : space.getSort();
    }

    private static Long spaceId(QuoteLine line) {
        return line.getSpace() == null ? null : line.getSpace().getSpaceId();
    }

    private static int lineSort(QuoteLine line) {
        return line.getSort() == null ? Integer.MAX_VALUE : line.getSort();
    }

    private static String regionKey(Space space) {
        if (space == null) {
            return "general";
        }
        if (space.getSpaceId() != null) {
            return "space-" + space.getSpaceId();
        }
        String name = normalizedText(space.getName());
        return name.isEmpty() ? "general" : "space-name-" + name;
    }

    private static String regionName(Space space) {
        if (space == null || space.getName() == null || space.getName().isBlank()) {
            return NO_REGION;
        }
        return space.getName().trim();
    }

    private static ItemType effectiveType(QuoteLine line) {
        return line.getType() == null ? ItemType.General : line.getType();
    }

    private static String displayCategory(QuoteLine line, ItemType type) {
        if (line.getCategory() != null && !line.getCategory().isBlank()) {
            return line.getCategory().trim();
        }
        return type.getName();
    }

    private static int typeOrder(ItemType type) {
        return switch (type == null ? ItemType.General : type) {
            case Construction -> 10;
            case Material -> 20;
            case Custom -> 30;
            case Soft -> 40;
            case Appliance -> 50;
            case General -> 60;
        };
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 金额缺失时保持不可确认，不能用 0 伪装为已知事实。 */
    private static BigDecimal sumNullable(BigDecimal total, BigDecimal value) {
        if (total == null || value == null) {
            return null;
        }
        return total.add(value);
    }

    private static String normalizedText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static boolean sameDecimal(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private static boolean sameText(String left, String right) {
        return normalizedText(left).equals(normalizedText(right));
    }

    private static final class TypeGroupAccumulator {
        private final String key;
        private final ItemType type;
        private final String category;
        private final boolean includeCost;
        private final Map<String, MergedLineAccumulator> rows = new LinkedHashMap<>();

        private TypeGroupAccumulator(String key, ItemType type, String category, boolean includeCost) {
            this.key = key;
            this.type = type;
            this.category = category;
            this.includeCost = includeCost;
        }

        private void merge(String mergeKey, QuoteLine line) {
            rows.computeIfAbsent(mergeKey, ignored -> new MergedLineAccumulator(type, category, line, includeCost)).add(line);
        }

        private QuoteGroup toGroup(Quote quote) {
            QuoteGroup group = new QuoteGroup();
            group.setKey(key);
            group.setName(category);
            group.setCategory(category);
            group.setType(type);
            group.setGroupMode(QuoteViewMode.TYPE);
            List<QuoteLine> mergedRows = rows.values().stream()
                    .map(row -> row.finish(quote))
                    .sorted(Comparator
                            .comparing((QuoteLine line) -> normalizedText(line.getName()))
                            .thenComparing(line -> normalizedText(line.getBrand()))
                            .thenComparing(line -> normalizedText(line.getSpec()))
                            .thenComparing(line -> normalizedText(line.getUnit() == null ? null : line.getUnit().name()))
                            .thenComparing(QuoteLine::getTypeRowId))
                    .toList();
            for (QuoteLine row : mergedRows) {
                group.setQuantity(group.getQuantity().add(value(row.getQuantity())));
                group.setAmount(sumNullable(group.getAmount(), row.getAmount()));
                group.setCost(sumNullable(group.getCost(), row.getCost()));
                group.getItems().add(row);
            }
            return group;
        }
    }

    private static final class MergedLineAccumulator {
        private final QuoteLine first;
        private final QuoteLine item;
        private final boolean includeCost;
        private final List<QuoteLine> sources = new ArrayList<>();
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
        private boolean amountKnown = true;
        private boolean costKnown = true;
        private boolean materialPriceSame = true;
        private boolean manualPriceSame = true;
        private boolean formulaSame = true;
        private boolean quantityFormulaSame = true;

        private MergedLineAccumulator(ItemType type, String category, QuoteLine first, boolean includeCost) {
            this.first = first;
            this.includeCost = includeCost;
            this.item = copySnapshot(first);
            this.item.setType(type);
            this.item.setCategory(category);
        }

        private void add(QuoteLine source) {
            sources.add(source);
            item.setQuantity(item.getQuantity().add(value(source.getQuantity())));
            if (source.getAmount() == null) {
                amountKnown = false;
            } else if (amountKnown) {
                amount = amount.add(source.getAmount());
            }
            if (!includeCost) {
                costKnown = false;
            } else if (source.getCost() == null) {
                costKnown = false;
            } else if (costKnown) {
                cost = cost.add(source.getCost());
            }
            item.setSort(Math.min(item.getSort() == null ? Integer.MAX_VALUE : item.getSort(), lineSort(source)));
            materialPriceSame &= sameDecimal(first.getMaterialPrice(), source.getMaterialPrice());
            manualPriceSame &= sameDecimal(first.getManualPrice(), source.getManualPrice());
            formulaSame &= sameText(first.getFormula(), source.getFormula());
            quantityFormulaSame &= sameText(first.getQuantityFormula(), source.getQuantityFormula());
        }

        private QuoteLine finish(Quote quote) {
            item.setMaterialPrice(materialPriceSame ? first.getMaterialPrice() : null);
            item.setManualPrice(manualPriceSame ? first.getManualPrice() : null);
            item.setAmount(amountKnown ? amount : null);
            item.setCost(includeCost && costKnown ? cost : null);
            item.setFormula(formulaSame ? first.getFormula() : null);
            item.setQuantityFormula(quantityFormulaSame ? first.getQuantityFormula() : null);
            item.setComponents(includeCost ? components() : componentsWithoutCost());
            item.setTypeRowId(QuoteViewContract.typeRowId(quote.getQuoteId(), quote.getVersion(), first));
            Sources sourcesView = sourcesView();
            item.setSourceLineIds(sourcesView.lineIds());
            item.setSourceRegionIds(sourcesView.regionIds());
            item.setSourceRegionNames(sourcesView.regionNames());
            item.setSourceCount(sourcesView.count());
            return item;
        }

        private List<QuoteComponent> componentsWithoutCost() {
            return sortedComponents(first).stream().map(MergedLineAccumulator::copyComponentWithoutCost).toList();
        }

        private List<QuoteComponent> components() {
            List<List<QuoteComponent>> all = sources.stream()
                    .map(MergedLineAccumulator::sortedComponents)
                    .toList();
            if (all.isEmpty() || all.get(0).isEmpty()) {
                return List.of();
            }
            int size = all.get(0).size();
            List<QuoteComponent> result = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                QuoteComponent firstComponent = all.get(0).get(index);
                QuoteComponent component = copyComponent(firstComponent);
                BigDecimal componentCost = BigDecimal.ZERO;
                boolean componentCostKnown = true;
                boolean formulaSame = true;
                for (List<QuoteComponent> sourceComponents : all) {
                    if (index >= sourceComponents.size()) {
                        continue;
                    }
                    QuoteComponent current = sourceComponents.get(index);
                    if (current.getCostAmount() == null) {
                        componentCostKnown = false;
                    } else if (componentCostKnown) {
                        componentCost = componentCost.add(current.getCostAmount());
                    }
                    formulaSame &= sameText(firstComponent.getFormula(), current.getFormula());
                }
                component.setCostAmount(componentCostKnown ? componentCost : null);
                component.setFormula(formulaSame ? firstComponent.getFormula() : null);
                result.add(component);
            }
            return result;
        }

        private Sources sourcesView() {
            List<Long> lineIds = sources.stream()
                    .map(QuoteLine::getItemId)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
            Map<String, RegionRef> regions = new LinkedHashMap<>();
            sources.stream()
                    .map(line -> new RegionRef(
                            line.getSpace() == null ? null : line.getSpace().getSpaceId(),
                            regionName(line.getSpace()),
                            regionSort(line),
                            regionKey(line.getSpace()),
                            line.getSpace() == null))
                    .sorted(Comparator
                            .comparing(RegionRef::general)
                            .thenComparingInt(RegionRef::sort)
                            .thenComparing(ref -> normalizedText(ref.name()))
                            .thenComparing(RegionRef::id, Comparator.nullsLast(Long::compareTo))
                            .thenComparing(RegionRef::key))
                    .forEach(ref -> regions.putIfAbsent(ref.key(), ref));
            List<RegionRef> ordered = new ArrayList<>(regions.values());
            return new Sources(
                    lineIds,
                    ordered.stream().map(RegionRef::id).toList(),
                    ordered.stream().map(RegionRef::name).toList(),
                    sources.size());
        }

        private static List<QuoteComponent> sortedComponents(QuoteLine line) {
            if (line.getComponents() == null || line.getComponents().isEmpty()) {
                return List.of();
            }
            return line.getComponents().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator
                            .comparing((QuoteComponent component) -> component.getSort() == null ? Integer.MAX_VALUE : component.getSort())
                            .thenComparing(component -> component.getType() == null ? "" : component.getType().name())
                            .thenComparing(component -> normalizedText(component.getName()))
                            .thenComparing(component -> value(component.getSaleAmount())))
                    .toList();
        }

        private static QuoteComponent copyComponentWithoutCost(QuoteComponent source) {
            QuoteComponent target = new QuoteComponent();
            target.setType(source.getType());
            target.setName(source.getName());
            target.setSaleAmount(source.getSaleAmount());
            target.setCostAmount(null);
            target.setFormula(null);
            target.setSort(source.getSort());
            return target;
        }

        private static QuoteComponent copyComponent(QuoteComponent source) {
            QuoteComponent target = new QuoteComponent();
            target.setType(source.getType());
            target.setName(source.getName());
            target.setSaleAmount(source.getSaleAmount());
            target.setCostAmount(BigDecimal.ZERO);
            target.setFormula(source.getFormula());
            target.setSort(source.getSort());
            return target;
        }
    }

    private record Sources(List<Long> lineIds, List<Long> regionIds, List<String> regionNames, int count) {}

    private record RegionRef(Long id, String name, int sort, String key, boolean general) {}
}
