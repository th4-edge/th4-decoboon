package com.th4.edge.decoboon.project.service.contractchange.batchsave;

import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 批量发起合同变更单。 */
public interface BatchSave extends Provider<ContractChange, ContractChange>, Security {
}
