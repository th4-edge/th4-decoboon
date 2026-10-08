package com.th4.edge.decoboon.designtemplate.service.templates.instantiate;

import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计模版实例化：按已发布版本生成设计方案的区域与分项（幂等）。 */
public interface Instantiate extends Provider<Design, Design>, Security {
}
