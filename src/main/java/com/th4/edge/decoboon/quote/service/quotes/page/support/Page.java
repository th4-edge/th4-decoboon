package com.th4.edge.decoboon.quote.service.quotes.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
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
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 客户报价分页实现。 */
@Note("客户报价分页")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "packageTemplate", "packageTemplateCode", "packageTemplateVersion", "packageTemplateSnapshotHash", "createdBy", "version", "mode",
                        "area", "subtotal", "amount", "cost", "costComplete", "designFee", "managementFee", "discount", "taxRate", "taxAmount", "status", "createTime", "property"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "area", "budget", "status"}),
                @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout"}),
@AllowProperties(model = com.th4.edge.decoboon.pack.model.PackageTemplate.class,
                properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price", "discount", "snapshotHash"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("quote/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.quote.service.quotes.page.Page {

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

    public Page(QuoteRepository quoteRepository, TenantContext tenantContext) {
        this.quoteRepository = quoteRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    public org.springframework.data.domain.Page<Quote> call(Quote request) {
        Employee currentEmployee = tenantContext.requireEmployee(employee);
        Long comId = currentEmployee.getCompany().getComId();
        Long empId = currentEmployee.getEmpId();
        List<Quote> quotes = switch (currentEmployee.getRole()) {
            case Boss -> quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(comId);
            case StoreManager -> currentEmployee.getStore() == null
                    || currentEmployee.getStore().getStoreId() == null
                    ? List.of()
                    : quoteRepository.findByCompany_ComIdAndCustomer_Store_StoreIdOrderByQuoteIdDesc(
                            comId, currentEmployee.getStore().getStoreId());
            case TeamLeader -> currentEmployee.getTeam() == null
                    || currentEmployee.getTeam().getTeamId() == null
                    ? List.of()
                    : quoteRepository.findByCompany_ComIdAndCustomer_Team_TeamIdOrderByQuoteIdDesc(
                            comId, currentEmployee.getTeam().getTeamId());
            case Designer -> throw new DefinedException(new DefaultErrorCode("403", "设计师请使用设计师报价列表服务"));
            case Marketer -> quoteRepository.findMarketerVisible(comId, empId);
            default -> List.of();
        };
        quotes = quotes.stream()
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
        return new PageImpl<>(quotes.subList(from, to), PageRequest.of(page, pageSize), quotes.size());
    }
}
