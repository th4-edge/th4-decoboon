package com.th4.edge.decoboon.carryover.service.reconciliation.attachment.page;

import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationAttachment;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 查看双轨对账附件。 */
public interface Page extends ModelService<CarryoverReconciliationAttachment, org.springframework.data.domain.Page<CarryoverReconciliationAttachment>>, Security {
}
