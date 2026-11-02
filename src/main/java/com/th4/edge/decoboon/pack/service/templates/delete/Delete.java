package com.th4.edge.decoboon.pack.service.templates.delete;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 套餐模板草稿删除。 */
public interface Delete extends Provider<PackageTemplate, Boolean>, Security {
}
