package com.th4.edge.decoboon.project.service.goodsreceipts.page;
import com.th4.edge.decoboon.project.model.GoodsReceipt;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
public interface Page extends Provider<GoodsReceipt,org.springframework.data.domain.Page<GoodsReceipt>>,Security{}
