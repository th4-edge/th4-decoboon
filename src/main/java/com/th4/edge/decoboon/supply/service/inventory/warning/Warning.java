package com.th4.edge.decoboon.supply.service.inventory.warning;

import com.th4.edge.decoboon.supply.model.InventoryStock;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Warning extends Provider<InventoryStock, java.util.List<InventoryStock>>, Security {
}
