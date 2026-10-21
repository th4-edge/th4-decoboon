package com.th4.edge.decoboon.finance.service.payables.report.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceSummaryAggregateContext;
import com.th4.edge.decoboon.finance.model.FinancePayableReport;
import com.th4.edge.decoboon.finance.model.Payable;
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

/** 应付筛选汇总实现。 */
@Note("应付账款汇总")
@Properties(allow = @AllowProperties(model = FinancePayableReport.class,
        properties = {"count", "totalAmount", "paidAmount", "outstandingAmount", "urgentAmount", "asOf"}))
@Service("finance/payable/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.payables.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String type;
    @AutoInject
    private String status;
    @AutoInject
    private Boolean urgent;
    @AutoInject
    private Long partnerId;
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
    public FinancePayableReport call(Payable request) {
        Employee current = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(current, storeId);
        FinanceSummaryAggregateContext.PayableAggregate aggregate = aggregateContext.payables(
                current, new FinanceSummaryAggregateContext.PayableCriteria(
                        projectId, type, status, urgent, partnerId, keyword, storeFilter));
        FinancePayableReport report = new FinancePayableReport();
        report.setCount(aggregate.count());
        report.setTotalAmount(aggregate.totalAmount());
        report.setPaidAmount(aggregate.paidAmount());
        report.setOutstandingAmount(aggregate.outstandingAmount());
        report.setUrgentAmount(aggregate.urgentAmount());
        report.setAsOf(aggregate.asOf());
        return report;
    }
}
