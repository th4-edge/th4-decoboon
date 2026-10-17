package com.th4.edge.decoboon.material;

import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.context.MaterialSheetPriceProjectionContext;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.material.repository.MaterialOrderSheetItemRepository;
import com.th4.edge.decoboon.material.repository.MaterialOrderSheetRepository;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** B62-DEF-001：价格状态必须按采购明细聚合，不能由最后一次下单覆盖。 */
class MaterialSheetPriceProjectionContextTest {

    private final MaterialOrderSheetItemRepository itemRepository = mock(MaterialOrderSheetItemRepository.class);
    private final PurchaseOrderItemRepository purchaseRepository = mock(PurchaseOrderItemRepository.class);
    private final MaterialOrderSheetRepository sheetRepository = mock(MaterialOrderSheetRepository.class);
    private final MaterialSheetPriceProjectionContext context =
            new MaterialSheetPriceProjectionContext(itemRepository, purchaseRepository, sheetRepository);

    @Test
    void pricedAndUnpricedItemsShouldProjectMixed() {
        MaterialOrderSheet sheet = new MaterialOrderSheet();
        MaterialOrderSheetItem line = line(sheet, 1L);
        when(purchaseRepository.findByMaterialPlan_PlanIdIn(any()))
                .thenReturn(List.of(item(1L, SheetPriceState.Priced, "100", null), item(1L, SheetPriceState.Unpriced, "0", null)));

        context.apply(sheet, List.of(line));

        assertThat(line.getPriceState()).isEqualTo(SheetPriceState.Mixed);
        assertThat(sheet.getPriceState()).isEqualTo(SheetPriceState.Mixed);
        assertThat(line.getPurchasePrice()).isEqualByComparingTo("100");
    }

    @Test
    void pricedAndSettledItemsShouldProjectMixed() {
        MaterialOrderSheet sheet = new MaterialOrderSheet();
        MaterialOrderSheetItem line = line(sheet, 2L);
        when(purchaseRepository.findByMaterialPlan_PlanIdIn(any()))
                .thenReturn(List.of(item(2L, SheetPriceState.Priced, "100", null), item(2L, SheetPriceState.Settled, "100", "120")));

        context.apply(sheet, List.of(line));

        assertThat(line.getPriceState()).isEqualTo(SheetPriceState.Mixed);
        assertThat(line.getSettlementPrice()).isNull();
    }

    @Test
    void allSettledSamePriceShouldProjectSettled() {
        MaterialOrderSheet sheet = new MaterialOrderSheet();
        MaterialOrderSheetItem line = line(sheet, 3L);
        when(purchaseRepository.findByMaterialPlan_PlanIdIn(any()))
                .thenReturn(List.of(item(3L, SheetPriceState.Settled, "100", "120"), item(3L, SheetPriceState.Settled, "100", "120")));

        context.apply(sheet, List.of(line));

        assertThat(line.getPriceState()).isEqualTo(SheetPriceState.Settled);
        assertThat(line.getSettlementPrice()).isEqualByComparingTo("120");
        assertThat(sheet.getPriceState()).isEqualTo(SheetPriceState.Settled);
    }

    @Test
    void persistShouldUpdateLineAndSheetProjection() {
        MaterialOrderSheet sheet = new MaterialOrderSheet();
        MaterialOrderSheetItem line = line(sheet, 4L);
        when(purchaseRepository.findByMaterialPlan_PlanIdIn(any()))
                .thenReturn(List.of(item(4L, SheetPriceState.Priced, "100", null), item(4L, SheetPriceState.Unpriced, "0", null)));

        context.persist(List.of(line));

        assertThat(line.getPriceState()).isEqualTo(SheetPriceState.Mixed);
        assertThat(sheet.getPriceState()).isEqualTo(SheetPriceState.Mixed);
    }

    private MaterialOrderSheetItem line(MaterialOrderSheet sheet, Long planId) {
        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(planId);
        if (sheet.getSheetId() == null) sheet.setSheetId(1000L + planId);
        MaterialOrderSheetItem line = new MaterialOrderSheetItem();
        line.setMaterialPlan(plan);
        line.setSheet(sheet);
        line.setPriceState(SheetPriceState.Unpriced);
        return line;
    }

    private PurchaseOrderItem item(Long planId, SheetPriceState state, String unitPrice, String settlementPrice) {
        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(planId);
        PurchaseOrder order = new PurchaseOrder();
        order.setStatus(com.th4.edge.decoboon.project.configure.PurchaseStatus.Ordered);
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(order);
        item.setMaterialPlan(plan);
        item.setPriceState(state);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setSettlementPrice(settlementPrice == null ? null : new BigDecimal(settlementPrice));
        return item;
    }
}
