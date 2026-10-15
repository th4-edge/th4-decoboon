package com.th4.edge.decoboon.material.service.sheet.report;

import com.th4.edge.decoboon.material.model.MaterialSheetReportRow;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 主材下单作业单品类汇总（聚合口径）。 */
public interface Report extends Provider<Employee, java.util.List<MaterialSheetReportRow>>, Security {
}
