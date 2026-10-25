package com.th4.edge.decoboon.quote.service.quotes.financereport.support;

import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteFinanceAggregateContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteDesignerAggregate;
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

import java.util.List;

/** 财务报价按设计师聚合实现（DEC-036 正模式：聚合 SQL 直接投影，不加载实体、不在 Java 循环求和）。 */
@Note("财务报价按设计师聚合")
@Properties(allow = @AllowProperties(model = QuoteDesignerAggregate.class,
        properties = {"designerId", "designerName", "quoteCount", "quoteAmount", "profitAmount", "costIncompleteCount"}))
@Service("quote/finance/report")
@Scope(ScopeTypes.PROTOTYPE)
public class FinanceReport implements com.th4.edge.decoboon.quote.service.quotes.financereport.FinanceReport {

    @User
    private Employee employee;

    @AutoInject
    private QuoteStatus status;

    private final DataScopeContext dataScopeContext;
    private final QuoteFinanceAggregateContext aggregateContext;

    public FinanceReport(DataScopeContext dataScopeContext, QuoteFinanceAggregateContext aggregateContext) {
        this.dataScopeContext = dataScopeContext;
        this.aggregateContext = aggregateContext;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteDesignerAggregate> call(Quote request) {
        Employee currentEmployee = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(currentEmployee);
        return aggregateContext.aggregate(scope, status);
    }
}
