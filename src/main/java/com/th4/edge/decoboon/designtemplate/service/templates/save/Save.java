package com.th4.edge.decoboon.designtemplate.service.templates.save;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计模版创建服务（同时创建第 1 版草稿）。 */
public interface Save extends Provider<DesignTemplate, DesignTemplate>, Security {
}
