package com.th4.edge.decoboon.property.service.regions.candidates;

import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 历史方案区域实测值 → 房屋区域实测档案候选清单（只读，人工确认后单独保存）。 */
public interface Candidates extends Provider<PropertyRegion, java.util.List<PropertyRegion>>, Security {
}
