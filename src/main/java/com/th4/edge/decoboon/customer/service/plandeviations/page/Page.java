package com.th4.edge.decoboon.customer.service.plandeviations.page;

import com.th4.edge.decoboon.project.model.ProjectPlanDeviation;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主授权项目计划偏差分页（只读）。 */
public interface Page extends Provider<ProjectPlanDeviation, org.springframework.data.domain.Page<ProjectPlanDeviation>>, Security {
}
