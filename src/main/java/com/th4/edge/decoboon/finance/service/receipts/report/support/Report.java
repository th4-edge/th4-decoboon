package com.th4.edge.decoboon.finance.service.receipts.report.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceSummaryAggregateContext;
import com.th4.edge.decoboon.finance.model.FinanceReceiptReport;
import com.th4.edge.decoboon.finance.model.Receipt;
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

/** 收款筛选汇总实现。 */
@Note("收款记录汇总")
@Properties(allow = @AllowProperties(model = FinanceReceiptReport.class,
        properties = {"receiptCount", "grossReceiptAmount", "reversalAmount", "receiptAmount", "asOf"}))
@Service("finance/receipt/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.receipts.report.Report {

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
    public FinanceReceiptReport call(Receipt request) {
        Employee current = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(current, storeId);
        FinanceSummaryAggregateContext.ReceiptAggregate aggregate = aggregateContext.receipts(
                current, new FinanceSummaryAggregateContext.ReceiptCriteria(projectId, keyword, storeFilter));
        FinanceReceiptReport report = new FinanceReceiptReport();
        report.setReceiptCount(aggregate.count());
        report.setGrossReceiptAmount(aggregate.totalAmount());
        report.setReversalAmount(aggregate.reversedAmount());
        report.setReceiptAmount(aggregate.netAmount());
        report.setAsOf(aggregate.asOf());
        return report;
    }
}
