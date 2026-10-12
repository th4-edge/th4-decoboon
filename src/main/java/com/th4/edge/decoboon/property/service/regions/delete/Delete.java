package com.th4.edge.decoboon.property.service.regions.delete;

import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 房屋区域实测档案删除（被草稿方案引用时阻断）。 */
public interface Delete extends Provider<PropertyRegion, PropertyRegion>, Security {
}
