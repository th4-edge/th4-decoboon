package com.th4.edge.decoboon.payroll.service.sheets.build;

import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Build extends ModelService<PayrollSheet, PayrollSheet>, Security {
}
