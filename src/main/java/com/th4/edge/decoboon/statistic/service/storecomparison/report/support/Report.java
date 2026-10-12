package com.th4.edge.decoboon.statistic.service.storecomparison.report.support;

import com.th4.edge.decoboon.statistic.context.StoreComparisonContext;
import com.th4.edge.decoboon.statistic.model.StoreComparisonQuery;
import com.th4.edge.decoboon.statistic.model.StoreComparisonReport;
import com.th4.edge.decoboon.statistic.model.StoreComparisonRow;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 门店对比矩阵实现（`P0D-05`）。
 *
 * <p>数据来源与单店视图同源（签约额/在建项目数与经营总览同口径，毛利复用 `BossDashboardContext.profitOf`），
 * 聚合 SQL 按门店 `group by` 下推；未归属门店单独成行、已停用门店展示但**不参与排名**。</p>
 */
@Note("门店对比矩阵")
@Description("按门店聚合对比签约额/回款额/应收应付/毛利/在建项目/客户数/人效；未归属与停用门店单独成行不参与排名；"
        + "禁止门店利润/独立核算/合并报表表达")
@Properties(allow = {
        @AllowProperties(model = StoreComparisonReport.class,
                properties = {"month", "period", "rankingNote", "rows", "total"}),
        @AllowProperties(model = StoreComparisonRow.class,
                properties = {"storeId", "storeName", "storeStatus", "unassigned", "ranked", "rank",
                        "contractAmount", "receiptAmount", "receivableAmount", "outstandingAmount",
                        "payableAmount", "unpaidAmount", "grossProfit", "grossMargin", "costComplete",
                        "costWarning", "activeProjectCount", "customerCount", "headcount", "perCapitaAmount"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("statistic/store-comparison/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.storecomparison.report.Report {

    @User
    private Employee employee;

    private final StoreComparisonContext comparisonContext;

    public Report(StoreComparisonContext comparisonContext) {
        this.comparisonContext = comparisonContext;
    }

    @Override
    @Transactional(readOnly = true)
    public StoreComparisonReport call(StoreComparisonQuery request) {
        return comparisonContext.report(employee, request == null ? null : request.getMonth());
    }
}
