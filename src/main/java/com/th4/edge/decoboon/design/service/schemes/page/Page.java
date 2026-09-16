package com.th4.edge.decoboon.design.service.schemes.page;

import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计方案分页。 */
public interface Page extends Provider<Design, org.springframework.data.domain.Page<Design>>, Security {
}
