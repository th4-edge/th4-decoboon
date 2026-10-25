package com.th4.edge.decoboon.trade.quote;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.configure.QuoteComponentType;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceStatus;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceType;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.configure.QuoteViewMode;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteViewContextTest {

    private final QuoteViewContext context = new QuoteViewContext();

    @Test
    void shouldBuildRegionAndTypeViewsFromSameSnapshot() {
        QuoteLine demolition = item(space(11L, "客厅", 1), ItemType.Construction, "拆除", "墙体拆除", "20");
        QuoteLine water = item(space(12L, "厨房", 2), ItemType.Construction, "水电", "水电改造", "80");
        QuoteLine tile = item(space(12L, "厨房", 2), ItemType.Material, "瓷砖", "厨房墙砖", "60");

        Quote quote = context.apply(quote(), List.of(water, tile, demolition));

        assertThat(quote.getItems()).containsExactly(water, tile, demolition);
        assertThat(quote.getRegionGroups()).extracting(QuoteGroup::getName)
                .containsExactly("客厅", "厨房");
        assertThat(quote.getRegionGroups()).extracting(QuoteGroup::getGroupMode)
                .containsOnly(QuoteViewMode.REGION);
        assertThat(quote.getRegionGroups().get(1).getAmount()).isEqualByComparingTo("140.00");
        assertThat(quote.getRegionGroups().get(1).getQuantity()).isEqualByComparingTo("2");
        assertThat(quote.getTypeGroups()).extracting(QuoteGroup::getGroupMode)
                .containsOnly(QuoteViewMode.TYPE);
        assertThat(quote.getConstructionGroups()).extracting(QuoteGroup::getName)
                .containsExactly("拆除", "水电");
        assertThat(quote.getMaterialGroups()).extracting(QuoteGroup::getName)
                .containsExactly("瓷砖");
    }

    @Test
    void shouldMergeOnlySameBusinessLinesAndKeepSortedSources() {
        QuoteLine living = item(space(11L, "客厅", 1), ItemType.Material, "地面", "木地板", "100");
        living.setItemId(30L);
        living.setBrand("A");
        living.setSpec("1200×150×18mm");
        living.setQuantity(new BigDecimal("12"));
        living.setAmount(new BigDecimal("1200.00"));
        living.setCost(new BigDecimal("1200.00"));

        QuoteLine bedroom = item(space(12L, "主卧", 2), ItemType.Material, "地面", " 木地板 ", "100");
        bedroom.setItemId(10L);
        bedroom.setBrand("A");
        bedroom.setSpec("1200×150×18mm");
        bedroom.setQuantity(new BigDecimal("8"));
        bedroom.setAmount(new BigDecimal("800.00"));
        bedroom.setCost(new BigDecimal("800.00"));

        QuoteLine pending = item(space(13L, "厨房", 3), ItemType.Material, "地面", "木地板", "100");
        pending.setItemId(20L);
        pending.setBrand("A");
        pending.setSpec("1200×150×18mm");
        pending.setDifferenceStatus(QuoteDifferenceStatus.Pending);
        pending.setQuantity(new BigDecimal("3"));
        pending.setAmount(new BigDecimal("300.00"));
        pending.setCost(new BigDecimal("300.00"));

        Quote quote = context.apply(quote(), List.of(living, bedroom, pending));

        QuoteGroup material = quote.getTypeGroups().stream()
                .filter(group -> group.getType() == ItemType.Material)
                .findFirst()
                .orElseThrow();
        assertThat(material.getItems()).hasSize(2);
        assertThat(material.getAmount()).isEqualByComparingTo("2300.00");
        assertThat(material.getQuantity()).isEqualByComparingTo("23");

        QuoteLine merged = material.getItems().stream()
                .filter(line -> line.getSourceCount() == 2)
                .findFirst()
                .orElseThrow();
        assertThat(merged.getItemId()).isNull();
        assertThat(merged.getTypeRowId()).startsWith("quote-type-");
        assertThat(merged.getQuantity()).isEqualByComparingTo("20");
        assertThat(merged.getAmount()).isEqualByComparingTo("2000.00");
        assertThat(merged.getCost()).isEqualByComparingTo("2000.00");
        assertThat(merged.getPrice()).isEqualByComparingTo("100");
        assertThat(merged.getSourceLineIds()).containsExactly(10L, 30L);
        assertThat(merged.getSourceRegionIds()).containsExactly(11L, 12L);
        assertThat(merged.getSourceRegionNames()).containsExactly("客厅", "主卧");
        assertThat(merged.getSourceCount()).isEqualTo(2);

        QuoteLine unmerged = material.getItems().stream()
                .filter(line -> line.getSourceCount() == 1)
                .findFirst()
                .orElseThrow();
        assertThat(unmerged.getDifferenceStatus()).isEqualTo(QuoteDifferenceStatus.Pending);

        Quote reverse = context.apply(quote(), List.of(pending, bedroom, living));
        QuoteLine reverseMerged = reverse.getTypeGroups().stream()
                .filter(group -> group.getType() == ItemType.Material)
                .flatMap(group -> group.getItems().stream())
                .filter(line -> line.getSourceCount() == 2)
                .findFirst()
                .orElseThrow();
        assertThat(reverseMerged.getTypeRowId()).isEqualTo(merged.getTypeRowId());
        assertThat(reverseMerged.getSourceLineIds()).containsExactly(10L, 30L);
    }

    @Test
    void managerViewShouldKeepRegionAndTypeFactsButHideRawItems() {
        QuoteLine item = item(space(11L, "客厅", 1), ItemType.Construction, "木工", "吊顶", "100");
        Quote quote = context.applyManagerView(quote(), List.of(item));

        assertThat(quote.getItems()).isNull();
        assertThat(quote.getRegionGroups()).hasSize(1);
        assertThat(quote.getRegionGroups().get(0).getGroupMode()).isEqualTo(QuoteViewMode.REGION);
        assertThat(quote.getTypeGroups()).hasSize(1);
        assertThat(quote.getTypeGroups().get(0).getGroupMode()).isEqualTo(QuoteViewMode.TYPE);
        assertThat(quote.getConstructionGroups()).hasSize(1);
        assertThat(quote.getConstructionGroups().get(0).getItems()).containsExactly(item);
    }

    @Test
    void shouldSumComponentCostWhenMergingIdenticalComponentSales() {
        QuoteLine living = item(space(11L, "客厅", 1), ItemType.Material, "地面", "木地板", "100");
        living.setComponents(List.of(component("10.00", "6.00")));
        QuoteLine bedroom = item(space(12L, "主卧", 2), ItemType.Material, "地面", "木地板", "100");
        bedroom.setComponents(List.of(component("10.00", "7.00")));

        Quote quote = context.apply(quote(), List.of(living, bedroom));
        QuoteLine merged = quote.getTypeGroups().get(0).getItems().get(0);

        assertThat(merged.getComponents()).singleElement().satisfies(component -> {
            assertThat(component.getSaleAmount()).isEqualByComparingTo("10.00");
            assertThat(component.getCostAmount()).isEqualByComparingTo("13.00");
        });
    }

    @Test
    void missingSourceAmountShouldRemainUnconfirmable() {
        QuoteLine living = item(space(11L, "客厅", 1), ItemType.Material, "地面", "木地板", "100");
        living.setItemId(30L);
        living.setAmount(null);
        QuoteLine bedroom = item(space(12L, "主卧", 2), ItemType.Material, "地面", "木地板", "100");
        bedroom.setItemId(10L);
        bedroom.setAmount(new BigDecimal("800.00"));

        Quote quote = context.apply(quote(), List.of(living, bedroom));
        QuoteGroup material = quote.getTypeGroups().stream()
                .filter(group -> group.getType() == ItemType.Material)
                .findFirst()
                .orElseThrow();
        QuoteLine merged = material.getItems().stream()
                .filter(line -> line.getSourceCount() == 2)
                .findFirst()
                .orElseThrow();

        assertThat(material.getAmount()).isNull();
        assertThat(merged.getAmount()).isNull();
        assertThat(merged.getQuantity()).isEqualByComparingTo("2");
    }

    @Test
    void unassignedRegionShouldStayLastEvenWhenOtherSpaceHasNoSort() {
        QuoteLine corridor = item(space(14L, "走廊", null), ItemType.Material, "地面", "木地板", "100");
        corridor.setItemId(14L);
        QuoteLine general = item(null, ItemType.Material, "地面", "木地板", "100");
        general.setItemId(9L);

        Quote quote = context.apply(quote(), List.of(general, corridor));
        QuoteLine merged = quote.getTypeGroups().stream()
                .flatMap(group -> group.getItems().stream())
                .filter(line -> line.getSourceCount() == 2)
                .findFirst()
                .orElseThrow();

        assertThat(merged.getSourceRegionIds()).containsExactly(14L, (Long) null);
        assertThat(merged.getSourceRegionNames()).containsExactly("走廊", "综合项目");
    }

    @Test
    void unassignedLinesShouldUseGeneralProjectBucket() {
        QuoteLine item = item(null, ItemType.Material, "辅材", "水泥", "50");
        Quote quote = context.apply(quote(), List.of(item));

        assertThat(quote.getRegionGroups()).singleElement().satisfies(group -> {
            assertThat(group.getName()).isEqualTo("综合项目");
            assertThat(group.getGroupMode()).isEqualTo(QuoteViewMode.REGION);
        });
        QuoteLine typeRow = quote.getTypeGroups().get(0).getItems().get(0);
        assertThat(typeRow.getSourceRegionIds()).containsExactly((Long) null);
        assertThat(typeRow.getSourceRegionNames()).containsExactly("综合项目");
    }

    private Quote quote() {
        Quote quote = new Quote();
        quote.setQuoteId(100L);
        quote.setVersion(2);
        return quote;
    }

    private Space space(Long id, String name, Integer sort) {
        Space space = new Space();
        space.setSpaceId(id);
        space.setName(name);
        space.setSort(sort);
        return space;
    }

    private QuoteComponent component(String saleAmount, String costAmount) {
        QuoteComponent component = new QuoteComponent();
        component.setType(QuoteComponentType.Material);
        component.setName("板材");
        component.setSaleAmount(new BigDecimal(saleAmount));
        component.setCostAmount(new BigDecimal(costAmount));
        component.setSort(1);
        return component;
    }

    private QuoteLine item(Space space, ItemType type, String category, String name, String amount) {
        QuoteLine item = new QuoteLine();
        item.setSpace(space);
        item.setType(type);
        item.setCategory(category);
        item.setName(name);
        item.setUnit(Unit.Area);
        item.setPricingMode(com.th4.edge.decoboon.catalog.configure.PricingMethod.MaterialAndManual);
        item.setQuantity(BigDecimal.ONE);
        item.setPrice(new BigDecimal(amount));
        item.setAmount(new BigDecimal(amount));
        item.setCost(new BigDecimal(amount));
        item.setRole(QuoteLineRole.Base);
        item.setDifferenceType(QuoteDifferenceType.None);
        item.setDifferenceStatus(QuoteDifferenceStatus.None);
        item.setIncluded(true);
        item.setDiscountAmount(BigDecimal.ZERO);
        item.setCraft("标准工艺");
        return item;
    }
}
