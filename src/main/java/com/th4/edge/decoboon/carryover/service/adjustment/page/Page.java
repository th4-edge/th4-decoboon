package com.th4.edge.decoboon.carryover.service.adjustment.page;

import com.th4.edge.decoboon.carryover.model.CarryoverAdjustment;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

public interface Page extends Provider<CarryoverAdjustment, List<CarryoverAdjustment>>, Security {
}
