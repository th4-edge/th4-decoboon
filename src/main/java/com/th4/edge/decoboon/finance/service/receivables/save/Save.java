package com.th4.edge.decoboon.finance.service.receivables.save;

import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 新建应收计划。 */
public interface Save extends ModelService<Receivable, Receivable>, Security {
}
