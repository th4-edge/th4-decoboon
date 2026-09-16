package com.th4.edge.decoboon.finance.service.settlements.retentionrelease;

import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Release extends Provider<Settlement, Settlement>, Security {
}
