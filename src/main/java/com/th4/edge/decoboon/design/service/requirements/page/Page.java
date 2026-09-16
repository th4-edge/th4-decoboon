package com.th4.edge.decoboon.design.service.requirements.page;

import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 客户诉求分页。 */
public interface Page extends Provider<Requirement, org.springframework.data.domain.Page<Requirement>>, Security {
}
