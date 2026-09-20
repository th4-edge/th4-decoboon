package com.th4.edge.decoboon.quote.service.quotes.designerinfo.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
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
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 设计师报价详情实现，禁止在响应中返回任何成本字段。 */
@Note("设计师报价详情")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "createdBy", "version", "mode",
                        "area", "subtotal", "amount", "designFee", "managementFee", "discount", "taxRate", "taxAmount", "status",
                        "submittedAt", "rejectedAt", "acceptedAt", "convertedAt", "items", "regionGroups", "constructionGroups",
                        "materialGroups", "otherGroups", "createTime", "updateTime", "property"}),
        @AllowProperties(model = QuoteGroup.class,
                properties = {"key", "name", "type", "category", "unit", "quantity", "amount", "items"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout", "coverUrl"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "formula", "craft", "criterion"}),
        @AllowProperties(model = QuoteLine.class,
                properties = {"itemId", "space", "type", "category", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "pricingMode", "price", "amount", "formula", "role",
                        "differenceType", "differenceStatus", "included", "discountAmount", "components", "sort"}),
        @AllowProperties(model = QuoteComponent.class,
                properties = {"type", "name", "saleAmount", "formula", "sort"}),
        @AllowProperties(model = com.th4.edge.decoboon.pack.model.PackageTemplate.class,
                properties = {"templateId", "name", "area", "price", "discount"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("quote/designer/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.quote.service.quotes.designerinfo.Info {

    @AutoInject
    @NotNull
    private Long quoteId;

    @User
    private Employee employee;

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository quoteLineRepository;
    private final DataScopeContext dataScopeContext;
    private final QuoteViewContext quoteViewContext;

    public Info(QuoteRepository quoteRepository,
                QuoteLineRepository quoteLineRepository,
                DataScopeContext dataScopeContext,
                QuoteViewContext quoteViewContext) {
        this.quoteRepository = quoteRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.dataScopeContext = dataScopeContext;
        this.quoteViewContext = quoteViewContext;
    }

    @Override
    public Quote call(Quote request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "仅设计师可访问设计师报价详情"));
        }
        DataScope scope = dataScopeContext.resolve(current);
        org.springframework.data.jpa.domain.Specification<Quote> byId =
                (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        Quote quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        if (quote.getCreatedBy() == null || !quote.getCreatedBy().getEmpId().equals(current.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问"));
        }
        return quoteViewContext.apply(quote, quoteLineRepository.findByQuote(quote));
    }
}
