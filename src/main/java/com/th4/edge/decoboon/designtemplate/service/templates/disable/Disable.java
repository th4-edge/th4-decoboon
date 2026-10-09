package com.th4.edge.decoboon.designtemplate.service.templates.disable;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 停用设计模版（不删除；历史报价不受影响）。 */
public interface Disable extends Provider<DesignTemplate, DesignTemplate>, Security {
}
