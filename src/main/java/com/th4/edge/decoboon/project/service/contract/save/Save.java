package com.th4.edge.decoboon.project.service.contract.save;

import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/** 生成项目合同基线。 */
public interface Save extends ModelService<ProjectContract, ProjectContract>, Security {
}
