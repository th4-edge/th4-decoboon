package com.th4.edge.decoboon.carryover.service.reconciliation.save;

import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationReport;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 创建/更新首月双轨对账批次。 */
public interface Save extends ModelService<CarryoverReconciliation, CarryoverReconciliationReport>, Security {
}
