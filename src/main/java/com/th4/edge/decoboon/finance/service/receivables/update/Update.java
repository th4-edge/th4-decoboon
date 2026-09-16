package com.th4.edge.decoboon.finance.service.receivables.update;

import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 更新应收计划。 */
public interface Update extends ModelService<Receivable, Receivable>, Security {
}
