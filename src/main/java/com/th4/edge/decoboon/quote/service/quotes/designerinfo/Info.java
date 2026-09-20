package com.th4.edge.decoboon.quote.service.quotes.designerinfo;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/** 设计师报价详情，响应不包含成本字段。 */
public interface Info extends ModelService<Quote, Quote>, Security {
}
