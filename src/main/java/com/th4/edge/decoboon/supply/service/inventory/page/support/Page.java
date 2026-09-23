package com.th4.edge.decoboon.supply.service.inventory.page.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.supply.context.InventoryContext;
import com.th4.edge.decoboon.supply.model.InventoryStock;
import com.th4.edge.decoboon.supply.repository.InventoryStockRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

/** 库存分页。 */
@Note("库存分页")
@Properties(allow = {
        @AllowProperties(model = InventoryStock.class,
                properties = {"stockId", "store", "item", "batchNo", "quantity", "lockedQuantity",
                        "availableQuantity", "unitCost", "safetyStock", "expiryDate", "lastInAt", "lastOutAt"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "code", "name", "brand", "series", "spec", "unit"})
})
@Service("inventory/stock/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.supply.service.inventory.page.Page {
    @User private Employee employee;
    private final TenantContext tenantContext;
    private final InventoryStockRepository stockRepository;
    private final InventoryContext inventoryContext;

    public Page(TenantContext tenantContext, InventoryStockRepository stockRepository,
                InventoryContext inventoryContext) {
        this.tenantContext = tenantContext;
        this.stockRepository = stockRepository;
        this.inventoryContext = inventoryContext;
    }

    @Override
    public org.springframework.data.domain.Page<InventoryStock> call(InventoryStock request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.Finance,
                EmployeeRole.StoreManager, EmployeeRole.Pm);
        var list = stockRepository.findByCompany_ComIdOrderByStockIdDesc(current.getCompany().getComId());
        if (current.getRole() == EmployeeRole.StoreManager && current.getStore() != null) {
            list = list.stream().filter(item -> item.getStore() != null
                    && item.getStore().getStoreId().equals(current.getStore().getStoreId())).toList();
        }
        list.forEach(item -> item.setAvailableQuantity(inventoryContext.available(item)));
        return new PageImpl<>(list);
    }
}
