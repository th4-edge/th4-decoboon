package com.th4.edge.decoboon.statistic.service.workbench.designer.queue.list;

import com.th4.edge.decoboon.statistic.model.WorkQueueItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 设计师今日主线队列。 */
public interface List extends Provider<Employee, java.util.List<WorkQueueItem>>, Security {
}
