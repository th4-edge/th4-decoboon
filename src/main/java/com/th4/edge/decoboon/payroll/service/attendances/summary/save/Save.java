package com.th4.edge.decoboon.payroll.service.attendances.summary.save;

import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Save extends ModelService<AttendanceSummary, AttendanceSummary>, Security {
}
