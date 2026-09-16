package com.th4.edge.decoboon.project.service.acceptances.save;

import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/**
 * 阶段验收提交服务。
 */
public interface Save extends Provider<Acceptance, Acceptance>, Security {
}
