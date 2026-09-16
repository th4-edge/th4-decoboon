package com.th4.edge.decoboon.finance.service.payments.save;

import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 新建业务付款。 */
public interface Save extends ModelService<Payment, Payment>, Security {
}
