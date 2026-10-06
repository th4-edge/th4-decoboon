package com.th4.edge.decoboon.statistic.service.metric.dictionary;

import com.th4.edge.decoboon.statistic.model.MetricDefinition;
import com.th4.edge.decoboon.statistic.model.MetricDictionaryQuery;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/** 对外指标口径字典。 */
public interface Dictionary extends Provider<MetricDictionaryQuery, List<MetricDefinition>>, Security {
}
