package com.th4.edge.decoboon.designtemplate.service.templates.version.build;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 已发布模版新建草稿版本（幂等：已有草稿则返回该草稿）。 */
public interface Build extends Provider<DesignTemplate, DesignTemplate>, Security {
}
