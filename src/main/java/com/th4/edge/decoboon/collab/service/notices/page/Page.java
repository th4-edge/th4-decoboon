package com.th4.edge.decoboon.collab.service.notices.page;

import com.th4.edge.decoboon.collab.model.Notice;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Page extends Provider<Notice, org.springframework.data.domain.Page<Notice>>, Security {
}
