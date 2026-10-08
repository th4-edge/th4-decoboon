package com.th4.edge.decoboon.quote.service.quotes.designerpage.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteBusinessStateContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 设计师或 BOSS 报价分页实现，禁止在响应中返回任何成本字段。 */
@Note("设计师报价分页")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "createdBy", "version", "mode",
                        "area", "subtotal", "amount", "designFee", "managementFee", "discount", "taxRate", "taxAmount", "status",
                        "submittedAt", "rejectedAt", "acceptedAt", "convertedAt", "createTime", "updateTime", "property", "businessStateView"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "area", "budget", "status"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout"}),
        @AllowProperties(model = com.th4.edge.decoboon.pack.model.PackageTemplate.class,
                properties = {"templateId", "name", "area", "price", "discount"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("quote/designer/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.quote.service.quotes.designerpage.Page {

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

    @AutoInject
    private String keyword;

    private final QuoteRepository quoteRepository;
    private final TenantContext tenantContext;
    private final QuoteBusinessStateContext quoteBusinessStateContext;

    public Page(QuoteRepository quoteRepository, TenantContext tenantContext,
                QuoteBusinessStateContext quoteBusinessStateContext) {
        this.quoteRepository = quoteRepository;
        this.tenantContext = tenantContext;
        this.quoteBusinessStateContext = quoteBusinessStateContext;
    }

    @Override
    public org.springframework.data.domain.Page<Quote> call(Quote request) {
        Employee currentEmployee = tenantContext.requireEmployee(employee);
        boolean boss = currentEmployee.getRole() == EmployeeRole.Boss;
        if (!boss && currentEmployee.getRole() != EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "仅设计师或企业负责人可访问设计与报价列表"));
        }
        Long comId = currentEmployee.getCompany().getComId();
        Long empId = currentEmployee.getEmpId();
        List<Quote> visibleQuotes = boss
                ? quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(comId)
                : quoteRepository.findByCompany_ComIdAndCreatedBy_EmpIdOrderByQuoteIdDesc(comId, empId);
        List<Quote> quotes = visibleQuotes
                .stream()
                .filter(quote -> customerId == null || quote.getCustomer().getCustId().equals(customerId))
                .filter(quote -> status == null || quote.getStatus() == status)
                .filter(quote -> keyword == null || keyword.isBlank()
                        || quote.getNo().toLowerCase().contains(keyword.trim().toLowerCase())
                        || (quote.getTitle() != null && quote.getTitle().toLowerCase().contains(keyword.trim().toLowerCase()))
                        || (quote.getCustomer() != null && quote.getCustomer().getName().toLowerCase().contains(keyword.trim().toLowerCase())))
                .sorted(Comparator.comparing(Quote::getQuoteId).reversed())
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 100 : Math.min(size, 200);
        int from = Math.min(page * pageSize, quotes.size());
        int to = Math.min(from + pageSize, quotes.size());
        List<Quote> pageQuotes = quotes.subList(from, to);
        pageQuotes.forEach(quoteBusinessStateContext::enrich);
        return new PageImpl<>(pageQuotes, PageRequest.of(page, pageSize), quotes.size());
    }
}
