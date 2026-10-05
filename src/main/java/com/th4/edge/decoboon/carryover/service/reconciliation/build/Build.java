package com.th4.edge.decoboon.carryover.service.reconciliation.build;

import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationExport;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 导出首月双轨对账表（UTF-8 CSV，可直接用 Excel 打开）。 */
public interface Build extends ModelService<CarryoverReconciliation, CarryoverReconciliationExport>, Security {
}
