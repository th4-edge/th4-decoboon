package com.th4.edge.decoboon.finance.service.payments.report.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceSummaryAggregateContext;
import com.th4.edge.decoboon.finance.model.FinancePaymentReport;
import com.th4.edge.decoboon.finance.model.Payment;
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

/** 付款筛选汇总实现。 */
@Note("付款记录汇总")
@Properties(allow = @AllowProperties(model = FinancePaymentReport.class,
        properties = {"paymentCount", "paymentAmount", "asOf"}))
@Service("finance/payment/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.payments.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
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
    public FinancePaymentReport call(Payment request) {
        Employee current = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(current, storeId);
        FinanceSummaryAggregateContext.PaymentAggregate aggregate = aggregateContext.payments(
                current, new FinanceSummaryAggregateContext.PaymentCriteria(projectId, keyword, storeFilter));
        FinancePaymentReport report = new FinancePaymentReport();
        report.setPaymentCount(aggregate.count());
        report.setPaymentAmount(aggregate.totalAmount());
        report.setAsOf(aggregate.asOf());
        return report;
    }
}
