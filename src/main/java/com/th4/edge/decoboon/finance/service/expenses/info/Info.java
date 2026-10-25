package com.th4.edge.decoboon.finance.service.expenses.info;

import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 费用/报销详情。 */
public interface Info extends ModelService<Expense, Expense>, Security { }
