package com.th4.edge.decoboon.property.service.measureregions.delete;

import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 量房原始区域删除（量房完成后冻结，DEC-066）。 */
public interface Delete extends Provider<MeasureRegion, MeasureRegion>, Security {
}
