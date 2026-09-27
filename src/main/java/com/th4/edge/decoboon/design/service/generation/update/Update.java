package com.th4.edge.decoboon.design.service.generation.update;

import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计师确认AI效果图版本。 */
public interface Update extends Provider<DesignGenerationTask, DesignGenerationTask>, Security {
}
