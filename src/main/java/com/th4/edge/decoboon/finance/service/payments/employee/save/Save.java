package com.th4.edge.decoboon.finance.service.payments.employee.save;

import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 员工工资/提成发放。 */
public interface Save extends ModelService<Payment, Payment>, Security {
}
