package com.th4.edge.decoboon.payroll.service.attendances.summary.page;

import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Page extends ModelService<AttendanceSummary, org.springframework.data.domain.Page<AttendanceSummary>>, Security {
}
