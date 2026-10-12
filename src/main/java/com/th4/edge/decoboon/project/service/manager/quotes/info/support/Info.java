package com.th4.edge.decoboon.project.service.manager.quotes.info.support;
import com.th4.edge.decoboon.design.model.Design;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectManagerAccessContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 项目经理负责项目来源报价详情实现。 */
@Note("项目经理来源报价详情")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "createdBy", "mode", "area", "subtotal",
                        "amount", "discount", "taxRate", "taxAmount", "status", "constructionGroups",
                        "materialGroups", "otherGroups", "createTime", "property"}),
        @AllowProperties(model = QuoteGroup.class,
                properties = {"key", "name", "type", "category", "unit", "quantity", "amount", "cost", "items"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "houseRegionId", "inheritedArea", "inheritedPerimeter", "houseValueStale"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "formula", "craft", "criterion"}),
        @AllowProperties(model = QuoteLine.class,
                properties = {"catalogItem", "itemId", "space", "type", "category", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "pricingMode", "price", "amount", "cost", "formula", "role",
                        "included", "discountAmount", "components", "craft", "sort"}),
        @AllowProperties(model = com.th4.edge.decoboon.quote.model.QuoteComponent.class,
                properties = {"type", "name", "saleAmount", "costAmount", "formula", "sort"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout"}),
        @AllowProperties(model = com.th4.edge.decoboon.pack.model.PackageTemplate.class,
                properties = {"templateId", "name", "area", "price", "discount"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status", "owner", "designer"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/manager/quote/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.project.service.manager.quotes.info.Info {

    @AutoInject
    @NotNull
    private Long quoteId;

    @User
    private Employee employee;

    private final ProjectManagerAccessContext accessContext;
    private final QuoteLineRepository quoteItemRepository;
    private final QuoteViewContext quoteViewContext;

    public Info(ProjectManagerAccessContext accessContext,
                QuoteLineRepository quoteItemRepository,
                QuoteViewContext quoteViewContext) {
        this.accessContext = accessContext;
        this.quoteItemRepository = quoteItemRepository;
        this.quoteViewContext = quoteViewContext;
    }

    @Override
    public Quote call(Quote request) {
        Quote quote = accessContext.requireQuote(quoteId, employee);
        return quoteViewContext.applyManagerView(quote, quoteItemRepository.findByQuote(quote));
    }
}
