package com.th4.edge.decoboon.payroll.service.compensations.page;

import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Page extends ModelService<CompensationScheme, org.springframework.data.domain.Page<CompensationScheme>>, Security {
}
