package com.th4.edge.decoboon.project.service.manager.quotes.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.project.context.ProjectManagerAccessContext;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 项目经理负责项目来源报价分页实现。 */
@Note("项目经理来源报价分页")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "createdBy", "mode", "area", "subtotal",
                        "amount", "discount", "taxRate", "taxAmount", "status", "createTime", "property"}),
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
@Service("project/manager/quote/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.manager.quotes.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private Long customerId;

    @AutoInject
    private QuoteStatus status;

    private final ProjectManagerAccessContext accessContext;

    public Page(ProjectManagerAccessContext accessContext) {
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Quote> call(Quote request) {
        List<Quote> quotes = accessContext.managedQuotes(employee).stream()
                .filter(quote -> customerId == null
                        || (quote.getCustomer() != null && customerId.equals(quote.getCustomer().getCustId())))
                .filter(quote -> status == null || quote.getStatus() == status)
                .sorted(Comparator.comparing(Quote::getQuoteId).reversed())
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 100 : Math.min(size, 200);
        int from = Math.min(page * pageSize, quotes.size());
        int to = Math.min(from + pageSize, quotes.size());
        return new PageImpl<>(quotes.subList(from, to), PageRequest.of(page, pageSize), quotes.size());
    }
}
