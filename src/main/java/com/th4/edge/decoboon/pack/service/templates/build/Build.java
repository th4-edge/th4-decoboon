package com.th4.edge.decoboon.pack.service.templates.build;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 基于当前最新套餐创建下一版草稿。 */
public interface Build extends Provider<PackageTemplate, PackageTemplate>, Security {
}
