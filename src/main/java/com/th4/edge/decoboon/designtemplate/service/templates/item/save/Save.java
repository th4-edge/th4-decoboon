package com.th4.edge.decoboon.designtemplate.service.templates.item.save;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计模版分项保存（仅草稿版本可写；不接收任何价格字段）。 */
public interface Save extends Provider<DesignTemplateItem, DesignTemplateItem>, Security {
}
