package com.th4.edge.decoboon.finance.service.payments.employee.page;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Page extends ModelService<Payable, org.springframework.data.domain.Page<Payable>>, Security {
}
