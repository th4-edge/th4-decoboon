package com.th4.edge.decoboon.project.service.plandeviations.page;

import com.th4.edge.decoboon.project.model.ProjectPlanDeviation;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 计划偏差分页服务。 */
public interface Page extends ModelService<ProjectPlanDeviation, org.springframework.data.domain.Page<ProjectPlanDeviation>>, Security {
}
