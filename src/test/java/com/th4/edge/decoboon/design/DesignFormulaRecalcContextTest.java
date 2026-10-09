package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.design.configure.QuantitySource;
import com.th4.edge.decoboon.design.context.DesignFormulaRecalcContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateFormulaContext;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesignFormulaRecalcContextTest {

    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final DesignItemRepository itemRepository = mock(DesignItemRepository.class);
    private final QuoteCalcContext quoteCalcContext = mock(QuoteCalcContext.class);
    private final DesignFormulaRecalcContext context = new DesignFormulaRecalcContext(
            spaceRepository, itemRepository, new DesignTemplateFormulaContext(), quoteCalcContext);

    @Test
    void recalculatesFormulaItemAndNeverOverwritesManualQuantity() {
        Design design = design(1L);
        Space space = space(10L, design, "客厅", "4", "5", "2.8");
        DesignItem formulaDriven = item(100L, design, space, "=WP", QuantitySource.Formula, "0");
        DesignItem manual = item(101L, design, space, "=WP", QuantitySource.Manual, "99");

        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(1L)).thenReturn(List.of(space));
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(1L)).thenReturn(List.of(formulaDriven, manual));
        when(itemRepository.save(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quoteCalcContext.calculate(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        context.recalculate(design);

        assertEquals(new BigDecimal("50.400"), formulaDriven.getQuantity());
        assertEquals("=WP", formulaDriven.getFormula());
        assertEquals(new BigDecimal("99"), manual.getQuantity());
        verify(quoteCalcContext).calculate(formulaDriven);
        verify(quoteCalcContext, never()).calculate(manual);
    }

    @Test
    void keepsPendingMarkerWhenSpaceDimensionsAreMissing() {
        Design design = design(2L);
        Space space = space(20L, design, "主卧", null, null, null);
        DesignItem item = item(200L, design, space, "=A", QuantitySource.Formula, "0");

        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(2L)).thenReturn(List.of(space));
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(2L)).thenReturn(List.of(item));
        when(itemRepository.save(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quoteCalcContext.calculate(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        context.recalculate(design);

        assertEquals(BigDecimal.ZERO, item.getQuantity());
        assertTrue(item.getFormula().startsWith(DesignTemplateFormulaContext.PENDING_MEASURE_MARKER));
    }

    @Test
    void recalculatesWholeHouseAggregateAcrossAllSpaces() {
        Design design = design(3L);
        Space living = space(30L, design, "客厅", "4", "5", "2.8");
        Space bedroom = space(31L, design, "主卧", "3", "4", "2.8");
        DesignItem aggregate = item(300L, design, null, "SUM(A)", QuantitySource.Formula, "0");

        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(3L)).thenReturn(List.of(living, bedroom));
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(3L)).thenReturn(List.of(aggregate));
        when(itemRepository.save(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quoteCalcContext.calculate(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        context.recalculate(design);

        assertEquals(new BigDecimal("32.000"), aggregate.getQuantity());
    }

    private Design design(Long id) {
        Design design = new Design();
        design.setDesignId(id);
        return design;
    }

    private Space space(Long id, Design design, String name, String length, String width, String height) {
        Space space = new Space();
        space.setSpaceId(id);
        space.setDesign(design);
        space.setName(name);
        space.setLength(decimal(length));
        space.setWidth(decimal(width));
        space.setHeight(decimal(height));
        return space;
    }

    private DesignItem item(Long id, Design design, Space space, String formula,
                           QuantitySource source, String quantity) {
        DesignItem item = new DesignItem();
        item.setItemId(id);
        item.setDesign(design);
        item.setSpace(space);
        item.setFormula(formula);
        item.setQuantitySource(source);
        item.setQuantity(decimal(quantity));
        return item;
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
