package com.th4.edge.decoboon.designtemplate.service.templates.copy;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 复制设计模版为当前员工的个人级草稿（来源记录在 sourceTemplate）。 */
public interface Copy extends Provider<DesignTemplate, DesignTemplate>, Security {
}
