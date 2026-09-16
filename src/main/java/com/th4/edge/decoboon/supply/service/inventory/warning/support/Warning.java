package com.th4.edge.decoboon.supply.service.inventory.warning.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.supply.context.InventoryContext;
import com.th4.edge.decoboon.supply.model.InventoryStock;
import com.th4.edge.decoboon.supply.repository.InventoryStockRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** 安全库存和临期预警。 */
@Note("库存预警")
@Properties(allow = {
        @AllowProperties(model = InventoryStock.class,
                properties = {"stockId", "store", "item", "batchNo", "quantity", "lockedQuantity",
                        "availableQuantity", "unitCost", "safetyStock", "expiryDate", "lastInAt", "lastOutAt"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "code", "name", "brand", "series", "spec", "unit"})
})
@Service("inventory/stock/warning")
@Scope(ScopeTypes.PROTOTYPE)
public class Warning implements com.th4.edge.decoboon.supply.service.inventory.warning.Warning {
    @User private Employee employee;
    private final TenantContext tenantContext;
    private final InventoryStockRepository stockRepository;
    private final InventoryContext inventoryContext;

    public Warning(TenantContext tenantContext, InventoryStockRepository stockRepository,
                   InventoryContext inventoryContext) {
        this.tenantContext = tenantContext;
        this.stockRepository = stockRepository;
        this.inventoryContext = inventoryContext;
    }

    @Override
    public List<InventoryStock> call(InventoryStock request) {
        Employee current = tenantContext.requireEmployee(employee);
        LocalDate threshold = LocalDate.now().plusDays(30);
        return stockRepository.findByCompany_ComIdOrderByStockIdDesc(current.getCompany().getComId()).stream()
                .peek(item -> item.setAvailableQuantity(inventoryContext.available(item)))
                .filter(item -> item.getAvailableQuantity().compareTo(item.getSafetyStock()) <= 0
                        || (item.getExpiryDate() != null && !item.getExpiryDate().isAfter(threshold)))
                .toList();
    }
}
