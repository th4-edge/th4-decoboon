package com.th4.edge.decoboon.finance.service.receivables.report.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceSummaryAggregateContext;
import com.th4.edge.decoboon.finance.model.FinanceReceivableReport;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 应收计划筛选汇总实现。 */
@Note("应收计划汇总")
@Properties(allow = @AllowProperties(model = FinanceReceivableReport.class,
        properties = {"count", "totalAmount", "receivedAmount", "outstandingAmount", "overdueAmount", "asOf"}))
@Service("finance/receivable/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.receivables.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String type;
    @AutoInject
    private String status;
    @AutoInject
    private String keyword;
    @AutoInject
    private Long storeId;

    private final FinanceAccessContext accessContext;
    private final FinanceSummaryAggregateContext aggregateContext;

    public Report(FinanceAccessContext accessContext, FinanceSummaryAggregateContext aggregateContext) {
        this.accessContext = accessContext;
        this.aggregateContext = aggregateContext;
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceReceivableReport call(Receivable request) {
        Employee current = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(current, storeId);
        FinanceSummaryAggregateContext.ReceivableFilteredAggregate aggregate = aggregateContext.receivables(
                current, new FinanceSummaryAggregateContext.ReceivableCriteria(
                        projectId, type, status, keyword, storeFilter));
        FinanceReceivableReport report = new FinanceReceivableReport();
        report.setCount(aggregate.count());
        report.setTotalAmount(aggregate.totalAmount());
        report.setReceivedAmount(aggregate.receivedAmount());
        report.setOutstandingAmount(aggregate.outstandingAmount());
        report.setOverdueAmount(aggregate.overdueAmount());
        report.setAsOf(aggregate.asOf());
        return report;
    }
}
