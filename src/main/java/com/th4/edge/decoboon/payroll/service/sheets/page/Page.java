package com.th4.edge.decoboon.payroll.service.sheets.page;

import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Page extends ModelService<PayrollSheet, org.springframework.data.domain.Page<PayrollSheet>>, Security {
}
