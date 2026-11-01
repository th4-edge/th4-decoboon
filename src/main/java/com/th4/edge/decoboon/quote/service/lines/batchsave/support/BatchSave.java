package com.th4.edge.decoboon.quote.service.lines.batchsave.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
import com.th4.edge.decoboon.quote.context.QuoteWorkbookContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
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
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** 设计师或 BOSS 的 Web 报价工作表兼容性入口。 */
@Note("报价工作表批量保存")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "packageTemplateCode", "packageTemplateVersion", "packageTemplateSnapshotHash", "createdBy", "version", "mode",
                        "area", "subtotal", "amount", "designFee", "managementFee", "discount", "taxRate", "taxAmount", "status",
                        "submittedAt", "rejectedAt", "acceptedAt", "convertedAt", "items", "regionGroups", "typeGroups", "constructionGroups",
                        "materialGroups", "otherGroups", "createTime", "updateTime", "property", "revision"}),
        @AllowProperties(model = QuoteLine.class,
                properties = {"itemId", "catalogSourceId", "catalogSourceCode", "catalogSourceRevision", "space", "type", "category", "packageSectionNameSnapshot", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "fixedTotalPrice", "pricingMode", "price", "amount", "formula",
                        "quantityFormula", "role", "differenceType", "differenceStatus", "included", "discountAmount",
                        "components", "craft", "sort", "typeRowId", "sourceLineIds", "sourceRegionIds", "sourceRegionNames", "sourceCount"}),
        @AllowProperties(model = QuoteGroup.class,
                properties = {"key", "name", "groupMode", "type", "category", "unit", "quantity", "amount", "items"}),
        @AllowProperties(model = QuoteComponent.class,
                properties = {"type", "name", "saleAmount", "formula", "sort"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout", "coverUrl"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "sourceMeasureRegionId"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "formula", "craft", "criterion"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.pack.model.PackageTemplate.class, properties = {"templateId", "code", "name", "version", "status", "area", "price", "discount", "currentPublished"})
})
@Service("quote/lines/save")
@Scope(ScopeTypes.PROTOTYPE)
public class BatchSave implements com.th4.edge.decoboon.quote.service.lines.batchsave.BatchSave {

    @AutoInject
    @NotNull
    private Long quoteId;

    /** 客户端读取时拿到的内容修订令牌；为空表示不做冲突校验（兼容旧客户端）。 */
    @AutoInject
    private String revisionToken;

    @ModelSaveInject
    private Quote request;

    @User
    private Employee employee;

    private final QuoteRepository quoteRepository;
    private final QuoteWorkbookContext workbookContext;
    private final QuoteViewContext viewContext;
    private final DataScopeContext dataScopeContext;

    public BatchSave(QuoteRepository quoteRepository,
                     QuoteWorkbookContext workbookContext,
                     QuoteViewContext viewContext,
                     DataScopeContext dataScopeContext) {
        this.quoteRepository = quoteRepository;
        this.workbookContext = workbookContext;
        this.viewContext = viewContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public Quote call(Quote ignored) {
        Employee current = dataScopeContext.requireEmployee(employee);
        boolean boss = current.getRole() == EmployeeRole.Boss;
        if (!boss && current.getRole() != EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "仅设计师或企业负责人可使用报价工作表编辑"));
        }
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        Quote quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        if (!boss && (quote.getCreatedBy() == null
                || !quote.getCreatedBy().getEmpId().equals(current.getEmpId()))) {
            throw new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问"));
        }
        Quote updated = workbookContext.update(quote, request, revisionToken);
        return viewContext.apply(updated, updated.getItems());
    }
}
