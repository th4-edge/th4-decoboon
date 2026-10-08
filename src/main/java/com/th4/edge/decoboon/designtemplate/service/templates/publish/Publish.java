package com.th4.edge.decoboon.designtemplate.service.templates.publish;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计模版发布服务（不设审批，但必须通过发布前校验）。 */
public interface Publish extends Provider<DesignTemplate, DesignTemplate>, Security {
}
