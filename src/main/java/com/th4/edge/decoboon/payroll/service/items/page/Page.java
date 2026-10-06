package com.th4.edge.decoboon.payroll.service.items.page;

import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Page extends ModelService<PayrollItem, org.springframework.data.domain.Page<PayrollItem>>, Security {
}
