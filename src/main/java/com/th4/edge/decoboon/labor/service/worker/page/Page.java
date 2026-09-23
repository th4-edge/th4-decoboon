package com.th4.edge.decoboon.labor.service.worker.page;

import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 工人档案分页。 */
public interface Page extends ModelService<Worker, org.springframework.data.domain.Page<Worker>>, Security {
}
