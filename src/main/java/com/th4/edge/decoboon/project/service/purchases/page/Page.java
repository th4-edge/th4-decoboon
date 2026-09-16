package com.th4.edge.decoboon.project.service.purchases.page;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
public interface Page extends Provider<PurchaseOrder,org.springframework.data.domain.Page<PurchaseOrder>>,Security{}
