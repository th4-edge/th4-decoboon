package com.th4.edge.decoboon.collab.service.notices.count;

import com.th4.edge.decoboon.collab.model.Notice;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Count extends Provider<Notice, Long>, Security {
}
