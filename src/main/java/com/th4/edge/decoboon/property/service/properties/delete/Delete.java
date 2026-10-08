package com.th4.edge.decoboon.property.service.properties.delete;

import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 删除没有任何附带数据的房屋（量房/诉求/方案/报价/合同/项目任一存在即阻断）。 */
public interface Delete extends Provider<Property, Property>, Security {
}
