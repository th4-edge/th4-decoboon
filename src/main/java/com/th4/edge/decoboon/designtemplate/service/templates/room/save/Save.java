package com.th4.edge.decoboon.designtemplate.service.templates.room.save;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计模版区域保存（仅草稿版本可写）。 */
public interface Save extends Provider<DesignTemplateRoom, DesignTemplateRoom>, Security {
}
