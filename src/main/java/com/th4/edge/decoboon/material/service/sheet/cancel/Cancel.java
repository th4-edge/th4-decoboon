package com.th4.edge.decoboon.material.service.sheet.cancel;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 取消主材下单作业单（未进入采购前；已下单走采购调整/退货）。 */
public interface Cancel extends Provider<MaterialOrderSheet, MaterialOrderSheet>, Security {
}
