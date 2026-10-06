package com.th4.edge.decoboon.statistic.service.workbench.quote.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.model.QuoteWorkbenchItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 设计师报价工作台实现，按报价所属房屋返回待跟进事实。 */
@Note("设计师报价工作台")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "version", "amount", "status", "createTime", "updateTime",
                        "submittedAt", "rejectedAt", "customer", "property"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "area", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "address", "area"})
})
@Service("statistic/workbench/quote/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.statistic.service.workbench.quote.page.Page {

    private final TenantContext tenantContext;
    private final QuoteRepository quoteRepository;

    public Page(TenantContext tenantContext, QuoteRepository quoteRepository) {
        this.tenantContext = tenantContext;
        this.quoteRepository = quoteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteWorkbenchItem> call(@User Employee employee) {
        Employee current = tenantContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        boolean boss = current.getRole() == EmployeeRole.Boss;
        List<QuoteStatus> statuses = List.of(QuoteStatus.Draft, QuoteStatus.Submitted, QuoteStatus.Rejected);

        List<Quote> activeQuotes = boss
                ? quoteRepository.findByCompany_ComIdAndStatusInOrderByQuoteIdDesc(comId, statuses)
                : quoteRepository.findByCompany_ComIdAndCreatedBy_EmpIdAndStatusInOrderByQuoteIdDesc(
                        comId, empId, statuses);
        if (activeQuotes.isEmpty()) {
            return List.of();
        }

        List<QuoteWorkbenchItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Quote quote : activeQuotes) {
            Customer customer = quote.getCustomer();
            Property property = quote.getProperty();
            if (customer == null || property == null) {
                continue;
            }

            QuoteWorkbenchItem item = new QuoteWorkbenchItem();
            item.setCustId(customer.getCustId());
            item.setPropertyId(property.getPropertyId());
            item.setCustomerName(customer.getName());
            item.setTelNo(customer.getTelNo());
            item.setCommunity(firstNonBlank(property.getCommunity(), customer.getCommunity()));
            item.setArea(property.getArea() == null ? customer.getArea() : property.getArea());
            item.setCustomerStatus(customer.getStatus().name());
            item.setCustomerStatusName(customer.getStatus().getName());

            item.setQuoteId(quote.getQuoteId());
            item.setQuoteNo(quote.getNo());
            item.setVersion(quote.getVersion());
            item.setAmount(quote.getAmount());
            item.setQuoteStatus(quote.getStatus().name());
            item.setQuoteStatusName(quote.getStatus().getName());
            item.setSubmittedAt(quote.getSubmittedAt() != null
                    ? quote.getSubmittedAt()
                    : quote.getCreateTime() == null ? null : quote.getCreateTime().toLocalDateTime());
            item.setLastFeedbackAt(quote.getRejectedAt() != null
                    ? quote.getRejectedAt()
                    : quote.getAcceptedAt());
            item.setAdjustmentCount(Math.max(0, (quote.getVersion() == null ? 1 : quote.getVersion()) - 1));

            LocalDateTime stageStart = statusStart(quote, now);
            item.setStayDays(Math.max(0, Duration.between(stageStart, now).toDays()));
            items.add(item);
        }

        items.sort(Comparator.comparing((QuoteWorkbenchItem item) -> !"Rejected".equals(item.getQuoteStatus()))
                .thenComparing(QuoteWorkbenchItem::getStayDays, Comparator.reverseOrder())
                .thenComparing(QuoteWorkbenchItem::getQuoteId, Comparator.reverseOrder()));
        return items;
    }

    private LocalDateTime statusStart(Quote quote, LocalDateTime now) {
        if (quote.getStatus() == QuoteStatus.Rejected && quote.getRejectedAt() != null) {
            return quote.getRejectedAt();
        }
        if (quote.getStatus() == QuoteStatus.Submitted && quote.getSubmittedAt() != null) {
            return quote.getSubmittedAt();
        }
        if (quote.getCreateTime() != null) {
            return quote.getCreateTime().toLocalDateTime();
        }
        return now;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
