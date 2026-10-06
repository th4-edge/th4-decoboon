package com.th4.edge.decoboon.payroll.service.payslips.page;

import com.th4.edge.decoboon.payroll.model.Payslip;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Page extends ModelService<Payslip, org.springframework.data.domain.Page<Payslip>>, Security {
}
