package com.th4.edge.decoboon.supply;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.supply.context.InventoryContext;
import com.th4.edge.decoboon.supply.model.InventoryStock;
import com.th4.edge.decoboon.supply.model.StockMovement;
import com.th4.edge.decoboon.supply.repository.InventoryStockRepository;
import com.th4.edge.decoboon.supply.repository.StockMovementRepository;
import com.th4.edge.framework.exception.DefinedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 库存领料、损耗和报价行成本回写测试。 */
class InventoryContextTest {

    private InventoryStockRepository stockRepository;
    private StockMovementRepository movementRepository;
    private QuoteLineRepository quoteLineRepository;
    private InventoryContext context;

    @BeforeEach
    void setUp() {
        stockRepository = mock(InventoryStockRepository.class);
        movementRepository = mock(StockMovementRepository.class);
        quoteLineRepository = mock(QuoteLineRepository.class);
        ModelIdentityContext identityContext = mock(ModelIdentityContext.class);
        when(identityContext.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        context = new InventoryContext(stockRepository, movementRepository, quoteLineRepository, identityContext);
    }

    @Test
    void shouldIssueMaterialWithoutDoubleCountingProcurementCost() {
        InventoryStock stock = stock();
        QuoteLine line = new QuoteLine();
        line.setActualMaterialCost(new BigDecimal("60"));
        line.setActualLaborCost(BigDecimal.ZERO);
        line.setActualOtherCost(BigDecimal.ZERO);
        line.setActualCost(new BigDecimal("60"));

        StockMovement movement = context.issue(stock, new Project(), line, null,
                new BigDecimal("3"), null, "OUT-1", List.of("photo.jpg"), "现场领料");

        assertThat(stock.getQuantity()).isEqualByComparingTo("7");
        assertThat(movement.getAmount()).isEqualByComparingTo("60");
        // 实际材料成本已在采购入库时入账，领料只流转库存，不得重复累计。
        assertThat(line.getActualMaterialCost()).isEqualByComparingTo("60");
        assertThat(line.getActualCost()).isEqualByComparingTo("60");
    }

    @Test
    void shouldRejectOutboundWhenStockInsufficient() {
        InventoryStock stock = stock();
        stock.setLockedQuantity(new BigDecimal("8"));

        assertThatThrownBy(() -> context.loss(stock, new Project(), null, null,
                new BigDecimal("3"), null, "LOSS-1", null, "损耗"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("库存不足");
    }

    private InventoryStock stock() {
        CatalogItem item = new CatalogItem();
        item.setItemId(1L);
        InventoryStock stock = new InventoryStock();
        stock.setStockId(1L);
        stock.setItem(item);
        stock.setQuantity(new BigDecimal("10"));
        stock.setLockedQuantity(BigDecimal.ZERO);
        stock.setUnitCost(new BigDecimal("20"));
        return stock;
    }
}
