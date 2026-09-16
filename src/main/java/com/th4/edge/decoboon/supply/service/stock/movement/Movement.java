package com.th4.edge.decoboon.supply.service.stock.movement;

import com.th4.edge.decoboon.supply.model.StockMovement;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Movement extends Provider<StockMovement, org.springframework.data.domain.Page<StockMovement>>, Security {
}
