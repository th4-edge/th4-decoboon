package com.th4.edge.decoboon.carryover.service.attachment.page;

import com.th4.edge.decoboon.carryover.model.CarryoverAttachment;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

public interface Page extends Provider<CarryoverAttachment, List<CarryoverAttachment>>, Security {
}
