package com.th4.edge.decoboon.material.service.settlement.cancel;

import com.th4.edge.decoboon.material.model.MaterialSettlement;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 撤销结算（纠错）：结算头保留留痕，采购/应付/成本冲回未结算状态。 */
public interface Cancel extends Provider<MaterialSettlement, MaterialSettlement>, Security {
}
