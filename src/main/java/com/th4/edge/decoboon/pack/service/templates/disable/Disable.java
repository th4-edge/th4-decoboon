package com.th4.edge.decoboon.pack.service.templates.disable;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 套餐模板停用。 */
public interface Disable extends Provider<PackageTemplate, PackageTemplate>, Security {
}
