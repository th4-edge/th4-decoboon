package com.th4.edge.decoboon.quote.service.lines.save.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceStatus;
import com.th4.edge.decoboon.quote.context.QuoteAdjustmentContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
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

import java.math.BigDecimal;

/** 报价草稿明细保存实现。 */
@Note("报价草稿明细调整")
@Properties(allow = {
        @AllowProperties(model = QuoteLine.class,
                properties = {"itemId", "quote", "space", "catalogItem", "role", "included", "discountAmount",
                        "differenceType", "differenceStatus", "components", "type", "category", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "pricingMode", "price", "amount", "formula", "quantityFormula", "sort"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "mode", "status", "area", "subtotal", "discount", "taxRate",
                        "taxAmount", "amount", "submittedAt", "rejectedAt", "acceptedAt", "convertedAt",
                        "property", "customer", "design", "packageTemplate"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "formula"}),
        @AllowProperties(model = Space.class, properties = {"spaceId", "name", "type"})
})
@Service("quote/line/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.quote.service.lines.save.Save {

    @AutoInject
    @NotNull
    private Long quoteId;

    @AutoInject
    private Long itemId;

    @AutoInject
    private Long baseLineId;

    @AutoInject
    private Long catalogItemId;

    @AutoInject
    private BigDecimal quantity;

    @AutoInject
    private Boolean deduct;

    @AutoInject
    private QuoteDifferenceStatus differenceStatus;

    @User
    private Employee employee;

    private final QuoteRepository quoteRepository;
    private final QuoteAdjustmentContext adjustmentContext;
    private final DataScopeContext dataScopeContext;

    public Save(QuoteRepository quoteRepository,
                QuoteAdjustmentContext adjustmentContext,
                DataScopeContext dataScopeContext) {
        this.quoteRepository = quoteRepository;
        this.adjustmentContext = adjustmentContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public QuoteLine call(@ModelSaveInject QuoteLine request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        Quote quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        request.setItemId(itemId);
        if (itemId != null && differenceStatus != null && !Boolean.TRUE.equals(deduct)
                && (catalogItemId == null || catalogItemId == 0)) {
            return adjustmentContext.updateDifference(quote, itemId, differenceStatus);
        }
        return adjustmentContext.save(quote, request, Boolean.TRUE.equals(deduct), baseLineId, catalogItemId, quantity);
    }
}
