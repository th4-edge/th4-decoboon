package com.th4.edge.decoboon.supply.service.inventory.page;

import com.th4.edge.decoboon.supply.model.InventoryStock;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Page extends Provider<InventoryStock, org.springframework.data.domain.Page<InventoryStock>>, Security {
}
