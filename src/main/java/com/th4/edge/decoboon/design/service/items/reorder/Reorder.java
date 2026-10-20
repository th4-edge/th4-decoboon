package com.th4.edge.decoboon.design.service.items.reorder;

import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计分项排序（只更新顺序，不重算或覆盖项目库快照）。 */
public interface Reorder extends Provider<Design, Design>, Security {
}
