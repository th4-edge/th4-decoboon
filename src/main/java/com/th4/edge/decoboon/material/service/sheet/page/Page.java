package com.th4.edge.decoboon.material.service.sheet.page;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/** 主材下单作业单列表 */
public interface Page extends Provider<MaterialOrderSheet, List<MaterialOrderSheet>>, Security {
}
