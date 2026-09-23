package com.th4.edge.decoboon.labor.service.worker.save;

import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 新增工人档案。 */
public interface Save extends ModelService<Worker, Worker>, Security {
}
