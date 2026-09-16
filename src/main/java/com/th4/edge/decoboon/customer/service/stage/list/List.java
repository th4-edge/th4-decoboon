package com.th4.edge.decoboon.customer.service.stage.list;

import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主授权项目阶段列表。 */
public interface List extends Provider<ProjectStage, java.util.List<ProjectStage>>, Security {
}
