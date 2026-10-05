package com.th4.edge.decoboon.carryover.service.reconciliation.update;

import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationReport;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 双轨对账状态流转：提交待确认、BOSS确认、BOSS关闭。 */
public interface Update extends ModelService<CarryoverReconciliation, CarryoverReconciliationReport>, Security {
}
