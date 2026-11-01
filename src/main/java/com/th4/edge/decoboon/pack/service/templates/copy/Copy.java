package com.th4.edge.decoboon.pack.service.templates.copy;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 复制最新套餐为独立编码的新草稿。 */
public interface Copy extends Provider<PackageTemplate, PackageTemplate>, Security {
}
