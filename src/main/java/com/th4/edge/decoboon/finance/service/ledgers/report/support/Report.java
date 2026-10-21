package com.th4.edge.decoboon.finance.service.ledgers.report.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceSummaryAggregateContext;
import com.th4.edge.decoboon.finance.model.FinanceLedgerReport;
import com.th4.edge.decoboon.finance.model.Ledger;
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

import java.time.LocalDate;
import java.util.List;

/** 资金台账筛选汇总实现。 */
@Note("资金台账汇总")
@Properties(allow = @AllowProperties(model = FinanceLedgerReport.class,
        properties = {"count", "totalAmount", "incomeAmount", "expenseAmount", "netCashFlowAmount", "asOf"}))
@Service("finance/ledger/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.ledgers.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String direction;
    @AutoInject
    private Long categoryId;
    @AutoInject
    private String category;
    /** 待关联业务分类：手工记账且尚未关联分类节点。 */
    @AutoInject
    private Boolean uncategorized;
    @AutoInject
    private LocalDate startDate;
    @AutoInject
    private LocalDate endDate;
    @AutoInject
    private String keyword;
    /** 生成凭证前，按已选流水主键汇总；与列表筛选条件互不冲突。 */
    @AutoInject
    private List<Long> ledgerIds;
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
    public FinanceLedgerReport call(Ledger request) {
        Employee current = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(current, storeId);
        FinanceSummaryAggregateContext.LedgerAggregate aggregate = aggregateContext.ledgers(
                current, new FinanceSummaryAggregateContext.LedgerCriteria(
                        projectId, direction, categoryId, category, uncategorized,
                        startDate, endDate, keyword, ledgerIds, storeFilter));
        FinanceLedgerReport report = new FinanceLedgerReport();
        report.setCount(aggregate.count());
        report.setTotalAmount(aggregate.totalAmount());
        report.setIncomeAmount(aggregate.incomeAmount());
        report.setExpenseAmount(aggregate.expenseAmount());
        report.setNetCashFlowAmount(aggregate.netCashFlowAmount());
        report.setAsOf(aggregate.asOf());
        return report;
    }
}
