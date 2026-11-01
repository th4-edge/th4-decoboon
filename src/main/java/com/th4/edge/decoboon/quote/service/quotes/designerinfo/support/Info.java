package com.th4.edge.decoboon.quote.service.quotes.designerinfo.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.context.QuoteBusinessStateContext;
import com.th4.edge.decoboon.quote.context.QuoteRevisionContext;
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

/** 设计师或 BOSS 报价详情实现，禁止在响应中返回任何成本字段。 */
@Note("设计师报价详情")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "packageTemplateCode", "packageTemplateVersion", "packageTemplateSnapshotHash", "createdBy", "version", "mode",
                        "area", "subtotal", "amount", "designFee", "managementFee", "discount", "taxRate", "taxAmount", "status",
                        "submittedAt", "rejectedAt", "acceptedAt", "acceptedBy", "convertedAt", "items", "regionGroups", "typeGroups", "constructionGroups",
                        "materialGroups", "otherGroups", "createTime", "updateTime", "property", "revision", "businessStateView"}),
        @AllowProperties(model = QuoteGroup.class,
                properties = {"key", "name", "groupMode", "type", "category", "unit", "quantity", "amount", "items"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout", "coverUrl"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "sourceMeasureRegionId"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "formula", "craft", "criterion"}),
        @AllowProperties(model = QuoteLine.class,
                properties = {"catalogItem", "itemId", "catalogSourceId", "catalogSourceCode", "catalogSourceRevision", "space", "type", "category", "packageSectionNameSnapshot", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "fixedTotalPrice", "pricingMode", "price", "amount", "formula", "quantityFormula", "role",
                        "differenceType", "differenceStatus", "included", "discountAmount", "components", "craft", "sort", "typeRowId", "sourceLineIds", "sourceRegionIds", "sourceRegionNames", "sourceCount"}),
        @AllowProperties(model = QuoteComponent.class,
                properties = {"type", "name", "saleAmount", "formula", "sort"}),
        @AllowProperties(model = com.th4.edge.decoboon.pack.model.PackageTemplate.class,
                properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price", "discount", "snapshotHash"}),
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
    private final QuoteRevisionContext revisionContext;
    private final QuoteBusinessStateContext quoteBusinessStateContext;

    public Info(QuoteRepository quoteRepository,
                QuoteLineRepository quoteLineRepository,
                DataScopeContext dataScopeContext,
                QuoteViewContext quoteViewContext,
                QuoteRevisionContext revisionContext,
                QuoteBusinessStateContext quoteBusinessStateContext) {
        this.quoteRepository = quoteRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.dataScopeContext = dataScopeContext;
        this.quoteViewContext = quoteViewContext;
        this.revisionContext = revisionContext;
        this.quoteBusinessStateContext = quoteBusinessStateContext;
    }

    @Override
    public Quote call(Quote request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        EmployeeRole role = current.getRole();
        boolean boss = role == EmployeeRole.Boss;
        // DEC-040：店长确认签约/生效前需要核对报价详情；BR-033：店长数据范围为本门店。
        if (!boss && role != EmployeeRole.Designer && role != EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "仅设计师、店长或企业负责人可访问设计与报价详情"));
        }
        DataScope scope = dataScopeContext.resolve(current);
        org.springframework.data.jpa.domain.Specification<Quote> byId =
                (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        Quote quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        if (role == EmployeeRole.Designer) {
            if (quote.getCreatedBy() == null
                    || !quote.getCreatedBy().getEmpId().equals(current.getEmpId())) {
                throw new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问"));
            }
        } else if (role == EmployeeRole.StoreManager) {
            Long storeId = current.getStore() == null ? null : current.getStore().getStoreId();
            Long quoteStoreId = quote.getCustomer() == null || quote.getCustomer().getStore() == null
                    ? null
                    : quote.getCustomer().getStore().getStoreId();
            if (storeId == null || !storeId.equals(quoteStoreId)) {
                throw new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问"));
            }
        }
        java.util.List<com.th4.edge.decoboon.quote.model.QuoteLine> lines = quoteLineRepository.findByQuote(quote);
        Quote view = quoteViewContext.apply(quote, lines);
        view.setRevision(revisionContext.token(quote, lines));
        quoteBusinessStateContext.enrich(view);
        return view;
    }
}
