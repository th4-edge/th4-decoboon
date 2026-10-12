package com.th4.edge.decoboon.statistic.service.storecomparison.report;

import com.th4.edge.decoboon.statistic.model.StoreComparisonQuery;
import com.th4.edge.decoboon.statistic.model.StoreComparisonReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 门店对比矩阵（`P0D-05`）：BOSS / 总部财务可见，同源同参、SQL 下推。 */
public interface Report extends Provider<StoreComparisonQuery, StoreComparisonReport>, Security {
}
