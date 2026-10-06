package com.th4.edge.decoboon.payroll.service.attendances.summary.delete;

import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 删除尚未进入工资单的月度考勤。 */
public interface Delete extends Provider<AttendanceSummary, AttendanceSummary>, Security {
}
