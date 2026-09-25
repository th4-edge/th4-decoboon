package com.th4.edge.decoboon.carryover.service.plan.change.page;

import com.th4.edge.decoboon.carryover.model.CarryoverPlanChange;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

public interface Page extends Provider<CarryoverPlanChange, List<CarryoverPlanChange>>, Security {
}
