package com.th4.edge.decoboon.project.service.contractchange.page;

import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/** 合同变更单分页。 */
public interface Page extends ModelService<ContractChange, org.springframework.data.domain.Page<ContractChange>>, Security {
}
