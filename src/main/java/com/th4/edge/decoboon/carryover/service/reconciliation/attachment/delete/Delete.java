package com.th4.edge.decoboon.carryover.service.reconciliation.attachment.delete;

import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationAttachment;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 删除双轨对账附件。 */
public interface Delete extends ModelService<CarryoverReconciliationAttachment, CarryoverReconciliationAttachment>, Security {
}
