package com.th4.edge.decoboon.finance.service.refunds.report.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceSummaryAggregateContext;
import com.th4.edge.decoboon.finance.model.FinanceRefundReport;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
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

/** 客户退款筛选汇总实现。 */
@Note("客户退款汇总")
@Properties(allow = @AllowProperties(model = FinanceRefundReport.class,
        properties = {"count", "totalAmount", "asOf"}))
@Service("finance/refund/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.refunds.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
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
    public FinanceRefundReport call(ReceiptRefund request) {
        Employee current = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(current, storeId);
        FinanceSummaryAggregateContext.RefundAggregate aggregate = aggregateContext.refunds(
                current, new FinanceSummaryAggregateContext.RefundCriteria(projectId, storeFilter));
        FinanceRefundReport report = new FinanceRefundReport();
        report.setCount(aggregate.count());
        report.setTotalAmount(aggregate.totalAmount());
        report.setAsOf(aggregate.asOf());
        return report;
    }
}
