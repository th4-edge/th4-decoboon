package com.th4.edge.decoboon.payroll.service.commissions.save;

import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Save extends ModelService<CommissionItem, CommissionItem>, Security {
}
