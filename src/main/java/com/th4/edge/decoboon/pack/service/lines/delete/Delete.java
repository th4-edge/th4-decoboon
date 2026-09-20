package com.th4.edge.decoboon.pack.service.lines.delete;

import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 套餐项目行删除。 */
public interface Delete extends Provider<PackageLine, Boolean>, Security {
}
