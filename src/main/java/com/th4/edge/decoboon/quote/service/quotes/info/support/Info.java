package com.th4.edge.decoboon.quote.service.quotes.info.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
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
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 报价详情实现。 */
@Note("报价详情")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "packageTemplateCode", "packageTemplateVersion", "packageTemplateSnapshotHash", "createdBy", "version", "mode",
                        "area", "subtotal", "amount", "cost", "costComplete", "designFee", "managementFee", "discount", "taxRate", "taxAmount", "status",
                        "items", "regionGroups", "typeGroups", "constructionGroups", "materialGroups", "otherGroups", "createTime", "property"}),
        @AllowProperties(model = QuoteGroup.class,
                properties = {"key", "name", "groupMode", "type", "category", "unit", "quantity", "amount", "cost", "items"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout", "coverUrl"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "sourceMeasureRegionId"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "formula", "craft", "criterion"}),
        @AllowProperties(model = QuoteLine.class,
                properties = {"catalogItem", "itemId", "catalogSourceId", "catalogSourceCode", "catalogSourceRevision", "space", "type", "category", "packageSectionNameSnapshot", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "fixedTotalPrice", "pricingMode", "price", "amount", "cost", "formula", "quantityFormula", "role",
                        "differenceType", "differenceStatus", "included", "discountAmount", "components", "craft", "sort", "typeRowId", "sourceLineIds", "sourceRegionIds", "sourceRegionNames", "sourceCount"}),
        @AllowProperties(model = com.th4.edge.decoboon.quote.model.QuoteComponent.class,
                properties = {"type", "name", "saleAmount", "costAmount", "formula", "sort"}),
        @AllowProperties(model = com.th4.edge.decoboon.pack.model.PackageTemplate.class,
                properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price", "discount", "snapshotHash"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
})
@Service("quote/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.quote.service.quotes.info.Info {

    @AutoInject
    @NotNull
    private Long quoteId;

    @User
    private Employee employee;

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository quoteItemRepository;
    private final DataScopeContext dataScopeContext;
    private final QuoteViewContext quoteViewContext;

    public Info(QuoteRepository quoteRepository,
                QuoteLineRepository quoteItemRepository,
                DataScopeContext dataScopeContext,
                QuoteViewContext quoteViewContext) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.dataScopeContext = dataScopeContext;
        this.quoteViewContext = quoteViewContext;
    }

    @Override
    public Quote call(Quote request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        if (current.getRole() == EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "设计师请使用设计师报价详情服务"));
        }
        Quote quote;
        if (current.getRole() == EmployeeRole.Marketer) {
            quote = quoteRepository.findMarketerVisibleById(quoteId, current.getCompany().getComId(), current.getEmpId())
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        } else {
            DataScope scope = dataScopeContext.resolve(current);
            org.springframework.data.jpa.domain.Specification<Quote> byId =
                    (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
            quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        }
        return quoteViewContext.apply(quote, quoteItemRepository.findByQuote(quote));
    }
}
