package com.th4.edge.decoboon.design.service.generation.build;

import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 构建AI设计上下文、Prompt和生成任务。 */
public interface Build extends Provider<DesignGenerationTask, DesignGenerationTask>, Security {
}
