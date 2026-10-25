package com.th4.edge.decoboon.statistic.service.designer.measure.page;

import com.th4.edge.decoboon.statistic.model.DesignerMeasureTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 设计师待量房任务分页入口。 */
public interface Page extends Provider<Employee, org.springframework.data.domain.Page<DesignerMeasureTask>>, Security {
}
