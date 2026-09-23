package com.th4.edge.decoboon.finance.service.aging.report.support;

import com.th4.edge.decoboon.finance.context.FinanceAgingContext;
import com.th4.edge.decoboon.finance.model.AgingBucket;
import com.th4.edge.decoboon.finance.model.FinanceAgingQuery;
import com.th4.edge.decoboon.finance.model.FinanceAgingReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 应收应付账龄报表实现。 */
@Note("应收应付账龄报表")
@Properties(allow = {
        @AllowProperties(model = FinanceAgingReport.class,
                properties = {"asOf", "receivableTotal", "receivableBuckets", "payableTotal", "payableBuckets",
                        "receivableOverdueRate", "payableOverdueRate"}),
        @AllowProperties(model = AgingBucket.class,
                properties = {"key", "label", "amount", "count", "level"})
})
@Service("finance/aging/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.aging.report.Report {

    @User
    private Employee employee;

    private final FinanceAgingContext agingContext;

    public Report(FinanceAgingContext agingContext) {
        this.agingContext = agingContext;
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceAgingReport call(FinanceAgingQuery request) {
        return agingContext.report(employee, request);
    }
}
