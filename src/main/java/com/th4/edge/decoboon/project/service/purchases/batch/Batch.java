package com.th4.edge.decoboon.project.service.purchases.batch;

import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Batch extends Provider<PurchaseOrder, PurchaseOrder>, Security {
}
