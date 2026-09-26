package com.th4.edge.decoboon.design.service.generation.page;

import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** AI设计生成任务分页。 */
public interface Page extends Provider<DesignGenerationTask, org.springframework.data.domain.Page<DesignGenerationTask>>, Security {
}
