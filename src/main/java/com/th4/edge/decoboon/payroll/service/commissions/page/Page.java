package com.th4.edge.decoboon.payroll.service.commissions.page;

import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Page extends ModelService<CommissionItem, org.springframework.data.domain.Page<CommissionItem>>, Security {
}
