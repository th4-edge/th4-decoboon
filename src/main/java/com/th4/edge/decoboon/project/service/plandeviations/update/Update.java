package com.th4.edge.decoboon.project.service.plandeviations.update;

import com.th4.edge.decoboon.project.model.ProjectPlanDeviation;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 计划偏差处理服务（确认 / 驳回 / 转合同工期变更）。 */
public interface Update extends Provider<ProjectPlanDeviation, ProjectPlanDeviation>, Security {
}
