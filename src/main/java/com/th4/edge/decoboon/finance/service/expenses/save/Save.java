package com.th4.edge.decoboon.finance.service.expenses.save;

import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 创建费用/报销草稿。 */
public interface Save extends ModelService<Expense, Expense>, Security { }
