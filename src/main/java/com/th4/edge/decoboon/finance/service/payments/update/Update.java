package com.th4.edge.decoboon.finance.service.payments.update;

import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 工资付款补录关联工人。 */
public interface Update extends Provider<Payment, Payment>, Security {
}
