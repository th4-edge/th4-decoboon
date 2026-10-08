package com.th4.edge.decoboon.statistic.service.designer.measure.list;

import com.th4.edge.decoboon.statistic.model.DesignerMeasureQuery;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计师待量房任务入口。 */
public interface List extends Provider<DesignerMeasureQuery, java.util.List<DesignerMeasureTask>>, Security {}
