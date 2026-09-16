package com.th4.edge.decoboon.supply.service.partner.page;

import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Page extends Provider<Partner, org.springframework.data.domain.Page<Partner>>, Security {
}
