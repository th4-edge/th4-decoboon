package com.th4.edge.decoboon.quote.service.lines.delete.support;

import com.th4.edge.decoboon.quote.context.QuoteAdjustmentContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** 报价草稿明细删除实现。 */
@Note("报价草稿明细删除")
@Service("quote/line/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.quote.service.lines.delete.Delete {

    @AutoInject
    @NotNull
    private Long quoteId;

    @AutoInject
    @NotNull
    private Long itemId;

    @User
    private Employee employee;

    private final QuoteRepository quoteRepository;
    private final QuoteAdjustmentContext adjustmentContext;
    private final DataScopeContext dataScopeContext;

    public Delete(QuoteRepository quoteRepository,
                  QuoteAdjustmentContext adjustmentContext,
                  DataScopeContext dataScopeContext) {
        this.quoteRepository = quoteRepository;
        this.adjustmentContext = adjustmentContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public Boolean call(QuoteLine request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        Quote quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        adjustmentContext.delete(quote, itemId);
        return true;
    }
}
