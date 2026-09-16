package com.th4.edge.decoboon.supply.service.partnerprice.page;

import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Page extends Provider<PartnerPrice, org.springframework.data.domain.Page<PartnerPrice>>, Security {
}
