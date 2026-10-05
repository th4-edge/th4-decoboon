package com.th4.edge.decoboon.carryover.service.reconciliation.item.save;

import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationItem;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationReport;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 保存双轨对账主题的原账金额、差异分类与处理。 */
public interface Save extends ModelService<CarryoverReconciliationItem, CarryoverReconciliationReport>, Security {
}
