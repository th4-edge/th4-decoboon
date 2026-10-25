package com.th4.edge.decoboon.finance.service.expenses.update;

import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 修改费用草稿并推进状态。 */
public interface Update extends ModelService<Expense, Expense>, Security { }
