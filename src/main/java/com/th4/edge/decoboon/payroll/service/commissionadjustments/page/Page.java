package com.th4.edge.decoboon.payroll.service.commissionadjustments.page;

import com.th4.edge.decoboon.payroll.model.CommissionAdjustment;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Page extends ModelService<CommissionAdjustment, org.springframework.data.domain.Page<CommissionAdjustment>>, Security {
}
