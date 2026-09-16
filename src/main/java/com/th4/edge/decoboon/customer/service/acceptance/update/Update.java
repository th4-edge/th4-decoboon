package com.th4.edge.decoboon.customer.service.acceptance.update;

import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主确认或驳回验收。 */
public interface Update extends Provider<Acceptance, Acceptance>, Security {
}
