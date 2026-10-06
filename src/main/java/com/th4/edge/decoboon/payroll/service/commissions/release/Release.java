package com.th4.edge.decoboon.payroll.service.commissions.release;

import com.th4.edge.decoboon.payroll.model.CommissionRelease;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Release extends ModelService<CommissionRelease, org.springframework.data.domain.Page<CommissionRelease>>, Security {
}
