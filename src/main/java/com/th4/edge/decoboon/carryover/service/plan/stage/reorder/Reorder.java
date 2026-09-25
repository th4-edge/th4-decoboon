package com.th4.edge.decoboon.carryover.service.plan.stage.reorder;

import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

public interface Reorder extends Provider<ProjectStage, List<ProjectStage>>, Security {
}
