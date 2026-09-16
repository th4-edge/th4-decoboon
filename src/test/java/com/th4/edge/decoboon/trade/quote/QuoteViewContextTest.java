package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteViewContextTest {

    private final QuoteViewContext context = new QuoteViewContext();

    @Test
    void shouldBuildRegionAndManagerViewsFromSameSnapshot() {
        Space living = space(11L, "客厅");
        Space kitchen = space(12L, "厨房");
        QuoteLine demolition = item(living, ItemType.Construction, "拆除", "墙体拆除", "20");
        QuoteLine water = item(kitchen, ItemType.Construction, "水电", "水电改造", "80");
        QuoteLine tile = item(kitchen, ItemType.Material, "瓷砖", "厨房墙砖", "60");

        Quote quote = context.apply(new Quote(), List.of(demolition, water, tile));

        assertThat(quote.getItems()).hasSize(3);
        assertThat(quote.getRegionGroups()).extracting(QuoteGroup::getName)
                .containsExactly("客厅", "厨房");
        assertThat(quote.getRegionGroups().get(1).getAmount()).isEqualByComparingTo("140.00");
        assertThat(quote.getConstructionGroups()).extracting(QuoteGroup::getName)
                .containsExactly("拆除", "水电");
        assertThat(quote.getMaterialGroups()).extracting(QuoteGroup::getName)
                .containsExactly("瓷砖");
    }

    @Test
    void managerViewShouldHideRegionStructure() {
        QuoteLine item = item(space(11L, "客厅"), ItemType.Construction, "木工", "吊顶", "100");
        Quote quote = context.applyManagerView(new Quote(), List.of(item));

        assertThat(quote.getItems()).isNull();
        assertThat(quote.getRegionGroups()).isNull();
        assertThat(quote.getConstructionGroups()).hasSize(1);
        assertThat(quote.getConstructionGroups().get(0).getItems()).containsExactly(item);
    }

    private Space space(Long id, String name) {
        Space space = new Space();
        space.setSpaceId(id);
        space.setName(name);
        return space;
    }

    private QuoteLine item(Space space, ItemType type, String category, String name, String amount) {
        QuoteLine item = new QuoteLine();
        item.setSpace(space);
        item.setType(type);
        item.setCategory(category);
        item.setName(name);
        item.setUnit(Unit.Area);
        item.setQuantity(BigDecimal.ONE);
        item.setPrice(new BigDecimal(amount));
        item.setAmount(new BigDecimal(amount));
        item.setCost(new BigDecimal(amount));
        return item;
    }
}
