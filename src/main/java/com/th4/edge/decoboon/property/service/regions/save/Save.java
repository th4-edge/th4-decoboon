package com.th4.edge.decoboon.property.service.regions.save;

import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 房屋区域实测档案保存（区域实测面积/周长的唯一事实源，DEC-064）。 */
public interface Save extends Provider<PropertyRegion, PropertyRegion>, Security {
}
