package com.th4.edge.decoboon.supply.service.labor.settle;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Settle extends Provider<Payable, Payable>, Security {
}
