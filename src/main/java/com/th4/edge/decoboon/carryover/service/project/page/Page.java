package com.th4.edge.decoboon.carryover.service.project.page;

import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

public interface Page extends ModelService<CarryoverProject, org.springframework.data.domain.Page<CarryoverProject>>, Security {
}
