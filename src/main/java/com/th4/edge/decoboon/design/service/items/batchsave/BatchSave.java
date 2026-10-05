package com.th4.edge.decoboon.design.service.items.batchsave;

import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计方案分项批量保存（整批成功或整批回滚）。 */
public interface BatchSave extends Provider<Design, Design>, Security {
}
