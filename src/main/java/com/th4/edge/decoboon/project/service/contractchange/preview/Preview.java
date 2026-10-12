package com.th4.edge.decoboon.project.service.contractchange.preview;

import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 预览增减项草稿，不落库。 */
public interface Preview extends Provider<ContractChange, ContractChange>, Security {
}
