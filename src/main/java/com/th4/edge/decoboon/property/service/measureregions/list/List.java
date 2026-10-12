package com.th4.edge.decoboon.property.service.measureregions.list;

import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 量房原始区域列表（按量房单，DEC-066）。 */
public interface List extends Provider<MeasureRegion, java.util.List<MeasureRegion>>, Security {
}
