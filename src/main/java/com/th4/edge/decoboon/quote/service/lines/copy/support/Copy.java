package com.th4.edge.decoboon.quote.service.lines.copy.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.context.QuoteAdjustmentContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
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

/** 报价分项复制实现。 */
@Note("报价分项复制")
@Properties(allow = {
        @AllowProperties(model = QuoteLine.class,
                properties = {"itemId", "space", "catalogItem", "role", "included", "discountAmount",
                        "differenceType", "differenceStatus", "components", "type", "category", "packageSectionNameSnapshot",
                        "name", "brand", "spec", "unit", "quantity", "materialPrice", "manualPrice", "fixedTotalPrice",
                        "pricingMode", "price", "amount", "formula", "quantityFormula", "sort"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "formula"}),
        @AllowProperties(model = Space.class, properties = {"spaceId", "name", "type"}),
        @AllowProperties(model = QuoteComponent.class,
                properties = {"type", "name", "saleAmount", "costAmount", "formula", "sort"})
})
@Service("quote/line/copy")
@Scope(ScopeTypes.PROTOTYPE)
public class Copy implements com.th4.edge.decoboon.quote.service.lines.copy.Copy {

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

    public Copy(QuoteRepository quoteRepository,
                QuoteAdjustmentContext adjustmentContext,
                DataScopeContext dataScopeContext) {
        this.quoteRepository = quoteRepository;
        this.adjustmentContext = adjustmentContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public QuoteLine call(QuoteLine request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        Quote quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        return adjustmentContext.copy(quote, itemId);
    }
}
