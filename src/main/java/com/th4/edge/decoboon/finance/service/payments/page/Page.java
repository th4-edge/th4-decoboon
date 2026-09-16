package com.th4.edge.decoboon.finance.service.payments.page;

import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 付款分页。 */
public interface Page extends ModelService<Payment, org.springframework.data.domain.Page<Payment>>, Security {
}
