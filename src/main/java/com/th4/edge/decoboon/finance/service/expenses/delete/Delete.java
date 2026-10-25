package com.th4.edge.decoboon.finance.service.expenses.delete;

import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 删除未提交费用草稿。 */
public interface Delete extends ModelService<Expense, Expense>, Security { }
