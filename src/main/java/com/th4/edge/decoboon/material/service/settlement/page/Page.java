package com.th4.edge.decoboon.material.service.settlement.page;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import com.th4.edge.decoboon.material.model.MaterialSettlement;
import java.util.List;

/** 主材采购结算单列表 */
public interface Page extends Provider<MaterialSettlement, List<MaterialSettlement>>, Security {
}
