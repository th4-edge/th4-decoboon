package com.th4.edge.decoboon.project.service.purchases.save;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
public interface Save extends Provider<PurchaseOrder,PurchaseOrder>,Security{}
