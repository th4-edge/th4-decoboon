package com.th4.edge.decoboon.project.service.contractchange.update;

import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/** 审批合同变更单。 */
public interface Update extends ModelService<ContractChange, ContractChange>, Security {
}
