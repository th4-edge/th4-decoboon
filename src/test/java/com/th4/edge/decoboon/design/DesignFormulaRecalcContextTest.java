package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.design.configure.AreaSource;
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

/**
 * 区域工程量重算口径测试（BR-040 §4 / DEC-062 / EV-213）。
 *
 * <p>面积与周长必须取**设计师实测录入值**；长宽只是量房快照，任何情况下都不得由长宽推算工程量；
 * 未录入或历史估算值一律按「待补尺寸」处理。</p>
 */
class DesignFormulaRecalcContextTest {

    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final DesignItemRepository itemRepository = mock(DesignItemRepository.class);
    private final QuoteCalcContext quoteCalcContext = mock(QuoteCalcContext.class);
    private final DesignFormulaRecalcContext context = new DesignFormulaRecalcContext(
            spaceRepository, itemRepository, new DesignTemplateFormulaContext(), quoteCalcContext);

    @Test
    void recalculatesFormulaItemFromMeasuredValuesAndNeverOverwritesManualQuantity() {
        Design design = design(1L);
        Space space = measuredSpace(10L, design, "客厅", "4", "5", "2.8", "21.50", "19.00");
        DesignItem formulaDriven = item(100L, design, space, "=WP", QuantitySource.Formula, "0");
        DesignItem manual = item(101L, design, space, "=WP", QuantitySource.Manual, "99");

        stub(1L, List.of(space), List.of(formulaDriven, manual));

        context.recalculate(design);

        // WP = 实测周长 19.00 × 层高 2.8（不扣门窗洞）
        assertEquals(new BigDecimal("53.200"), formulaDriven.getQuantity());
        assertEquals("=WP", formulaDriven.getFormula());
        assertEquals(new BigDecimal("99"), manual.getQuantity());
        verify(quoteCalcContext).calculate(formulaDriven);
        verify(quoteCalcContext, never()).calculate(manual);
    }

    @Test
    void usesMeasuredAreaDirectlyAndIgnoresSnapshotLengthWidth() {
        Design design = design(2L);
        Space space = measuredSpace(20L, design, "主卧", "3", "4", "2.8", "21.50", "19.00");
        DesignItem item = item(200L, design, space, "=A", QuantitySource.Formula, "0");

        stub(2L, List.of(space), List.of(item));
        context.recalculate(design);

        // 实测面积 21.50，而不是量房快照 3 × 4 = 12
        assertEquals(new BigDecimal("21.500"), item.getQuantity());

        // 只改量房快照长宽，不改变工程量
        space.setLength(new BigDecimal("9"));
        space.setWidth(new BigDecimal("9"));
        stub(2L, List.of(space), List.of(item));
        context.recalculate(design);

        assertEquals(new BigDecimal("21.500"), item.getQuantity());
    }

    @Test
    void keepsPendingMarkerWhenMeasuredValuesAreMissing() {
        Design design = design(3L);
        Space space = space(30L, design, "主卧", "4", "5", "2.8", null, null, null);
        DesignItem item = item(300L, design, space, "=A", QuantitySource.Formula, "0");

        stub(3L, List.of(space), List.of(item));
        context.recalculate(design);

        assertEquals(BigDecimal.ZERO, item.getQuantity());
        assertTrue(item.getFormula().startsWith(DesignTemplateFormulaContext.PENDING_MEASURE_MARKER));
    }

    @Test
    void keepsPendingMarkerForLegacyEstimatedValues() {
        Design design = design(4L);
        Space space = space(40L, design, "次卧", "4", "5", "2.8", "20.00", "18.00", AreaSource.Estimated);
        DesignItem item = item(400L, design, space, "=A", QuantitySource.Formula, "0");

        stub(4L, List.of(space), List.of(item));
        context.recalculate(design);

        // 历史按长宽推算的估算值不得继续充当工程量基数
        assertEquals(BigDecimal.ZERO, item.getQuantity());
        assertTrue(item.getFormula().startsWith(DesignTemplateFormulaContext.PENDING_MEASURE_MARKER));
    }

    @Test
    void recalculatesWholeHouseAggregateFromMeasuredAreas() {
        Design design = design(5L);
        Space living = measuredSpace(50L, design, "客厅", "4", "5", "2.8", "20.50", "18.00");
        Space bedroom = measuredSpace(51L, design, "主卧", "3", "4", "2.8", "12.25", "14.00");
        DesignItem aggregate = item(500L, design, null, "SUM(A)", QuantitySource.Formula, "0");

        stub(5L, List.of(living, bedroom), List.of(aggregate));
        context.recalculate(design);

        assertEquals(new BigDecimal("32.750"), aggregate.getQuantity());
    }

    @Test
    void wholeHouseAggregateIsPendingWhenAnySpaceHasNoMeasuredArea() {
        Design design = design(6L);
        Space measured = measuredSpace(60L, design, "客厅", "4", "5", "2.8", "20.50", "18.00");
        Space pending = space(61L, design, "主卧", "3", "4", "2.8", null, null, null);
        DesignItem aggregate = item(600L, design, null, "SUM(A)", QuantitySource.Formula, "0");

        stub(6L, List.of(measured, pending), List.of(aggregate));
        context.recalculate(design);

        // 不得用部分区域求和的偏小值冒充已完成工程量
        assertEquals(BigDecimal.ZERO, aggregate.getQuantity());
        assertTrue(aggregate.getFormula().startsWith(DesignTemplateFormulaContext.PENDING_MEASURE_MARKER));
    }

    private void stub(Long designId, List<Space> spaces, List<DesignItem> items) {
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(designId)).thenReturn(spaces);
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(designId)).thenReturn(items);
        when(itemRepository.save(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quoteCalcContext.calculate(any(DesignItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Design design(Long id) {
        Design design = new Design();
        design.setDesignId(id);
        return design;
    }

    private Space measuredSpace(Long id, Design design, String name, String length, String width,
                                String height, String area, String perimeter) {
        return space(id, design, name, length, width, height, area, perimeter, AreaSource.Measured);
    }

    private Space space(Long id, Design design, String name, String length, String width, String height,
                        String area, String perimeter, AreaSource source) {
        Space space = new Space();
        space.setSpaceId(id);
        space.setDesign(design);
        space.setName(name);
        space.setLength(decimal(length));
        space.setWidth(decimal(width));
        space.setHeight(decimal(height));
        space.setArea(decimal(area));
        space.setPerimeter(decimal(perimeter));
        space.setAreaSource(source);
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
