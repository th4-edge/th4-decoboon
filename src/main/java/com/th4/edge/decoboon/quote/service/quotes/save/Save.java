package com.th4.edge.decoboon.quote.service.quotes.save;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/**
 * 客户报价创建服务。
 */
public interface Save extends Provider<Quote, Quote>, Security {
}
