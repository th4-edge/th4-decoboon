package com.th4.edge.decoboon.carryover.service.reconciliation.attachment.save;

import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationAttachment;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 保存双轨对账附件。 */
public interface Save extends ModelService<CarryoverReconciliationAttachment, CarryoverReconciliationAttachment>, Security {
}
