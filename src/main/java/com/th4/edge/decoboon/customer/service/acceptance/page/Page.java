package com.th4.edge.decoboon.customer.service.acceptance.page;

import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主授权项目验收分页。 */
public interface Page extends Provider<Acceptance, org.springframework.data.domain.Page<Acceptance>>, Security {
}
