package com.th4.edge.decoboon.quote.service.quotes.financesummary.support;

import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteFinanceAggregateContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteFinanceSummary;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 财务报价单行汇总实现。 */
@Note("财务报价汇总")
@Properties(allow = @AllowProperties(model = QuoteFinanceSummary.class,
        properties = {"asOf", "quoteCount", "quoteAmount", "profitAmount", "costIncompleteCount"}))
@Service("quote/finance/summary")
@Scope(ScopeTypes.PROTOTYPE)
public class FinanceSummary implements com.th4.edge.decoboon.quote.service.quotes.financesummary.FinanceSummary {

    @User
    private Employee employee;

    @AutoInject
    private QuoteStatus status;

    private final DataScopeContext dataScopeContext;
    private final QuoteFinanceAggregateContext aggregateContext;

    public FinanceSummary(DataScopeContext dataScopeContext, QuoteFinanceAggregateContext aggregateContext) {
        this.dataScopeContext = dataScopeContext;
        this.aggregateContext = aggregateContext;
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteFinanceSummary call(Quote request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        return aggregateContext.summary(scope, status);
    }
}
