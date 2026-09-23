package com.th4.edge.decoboon.labor.service.worker.update;

import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 编辑或停用工人档案。 */
public interface Update extends ModelService<Worker, Worker>, Security {
}
