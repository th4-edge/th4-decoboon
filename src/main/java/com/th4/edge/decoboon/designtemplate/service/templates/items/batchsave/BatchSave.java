package com.th4.edge.decoboon.designtemplate.service.templates.items.batchsave;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计模版分项批量保存（整批成功或整批回滚，对齐设计工作区的 `design/items/save`）。 */
public interface BatchSave extends Provider<DesignTemplate, DesignTemplate>, Security {
}
