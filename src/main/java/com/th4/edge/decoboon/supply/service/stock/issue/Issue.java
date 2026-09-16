package com.th4.edge.decoboon.supply.service.stock.issue;

import com.th4.edge.decoboon.supply.model.StockMovement;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Issue extends Provider<StockMovement, StockMovement>, Security {
}
