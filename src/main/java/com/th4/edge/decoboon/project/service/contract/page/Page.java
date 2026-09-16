package com.th4.edge.decoboon.project.service.contract.page;

import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/** 项目合同基线分页。 */
public interface Page extends ModelService<ProjectContract, org.springframework.data.domain.Page<ProjectContract>>, Security {
}
