package com.th4.edge.decoboon.design.service.talks.page;

import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 谈单 PPT 分页。 */
public interface Page extends Provider<Talk, org.springframework.data.domain.Page<Talk>>, Security {
}
