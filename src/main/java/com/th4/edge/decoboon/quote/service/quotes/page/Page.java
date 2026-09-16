package com.th4.edge.decoboon.quote.service.quotes.page;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/**
 * 客户报价分页服务。
 */
public interface Page extends ModelService<Quote, org.springframework.data.domain.Page<Quote>>, Security {
}
