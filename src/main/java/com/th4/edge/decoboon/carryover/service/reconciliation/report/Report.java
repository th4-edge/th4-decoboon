package com.th4.edge.decoboon.carryover.service.reconciliation.report;

import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationReport;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 查看双轨对账批次与四主题。 */
public interface Report extends ModelService<CarryoverReconciliation, CarryoverReconciliationReport>, Security {
}
