package com.th4.edge.decoboon.quote.service.lines.page.support;
import com.th4.edge.decoboon.design.model.Space;

import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.service.annotation.ServiceName;
import com.th4.edge.framework.service.page.context.AbstractPage;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Scope;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;

/** 报价明细分页实现。 */
@Note("报价明细分页")
@Properties(allow = {
        @AllowProperties(model = QuoteLine.class,
                properties = {"itemId", "catalogSourceId", "catalogSourceCode", "catalogSourceRevision", "space", "type", "category", "packageSectionNameSnapshot", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "pricingMode", "price", "amount", "cost", "formula", "quantityFormula", "role",
                        "differenceType", "differenceStatus", "included", "discountAmount", "components", "craft", "sort"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "sourceMeasureRegionId"}),
        @AllowProperties(model = com.th4.edge.decoboon.quote.model.QuoteComponent.class,
                properties = {"type", "name", "saleAmount", "costAmount", "formula", "sort"})
})
@Service("quote/line/page")
@ServiceName("page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page extends AbstractPage<QuoteLine> implements com.th4.edge.decoboon.quote.service.lines.page.Page {
    @AutoInject
    private Long quoteId;
    @User
    private Employee employee;
    private final QuoteRepository quoteRepository;
    private final DataScopeContext dataScopeContext;

    @Override
    public org.springframework.data.domain.Page<QuoteLine> call(QuoteLine request) {
        if (quoteId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "报价索引不能为空"));
        }
        Employee current = dataScopeContext.requireEmployee(employee);
        Quote quote;
        if (current.getRole() == EmployeeRole.Marketer) {
            quote = quoteRepository.findMarketerVisibleById(quoteId, current.getCompany().getComId(), current.getEmpId())
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        } else {
            DataScope scope = dataScopeContext.resolve(current);
            Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
            quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        }
        request.setCompany(quote.getCompany());
        request.setQuote(quote);
        return super.call(request);
    }

    public Page(EntityManager entityManager, QuoteRepository quoteRepository,
                DataScopeContext dataScopeContext) {
        super(entityManager);
        this.quoteRepository = quoteRepository;
        this.dataScopeContext = dataScopeContext;
    }
}
