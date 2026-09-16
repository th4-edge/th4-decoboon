package com.th4.edge.decoboon.project.service.stages.page;

import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/** 阶段分页服务。 */
public interface Page extends ModelService<ProjectStage, org.springframework.data.domain.Page<ProjectStage>>, Security {
}
