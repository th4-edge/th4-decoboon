package com.th4.edge.decoboon.statistic.service.workbench.quote.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.context.DesignerWorkbenchRisk;
import com.th4.edge.decoboon.statistic.model.QuoteWorkbenchItem;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 设计师报价工作台：数据库分页后仅批量组装当前页，禁止全量报价加载与内存筛页。 */
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

    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private String keyword;
    /** all / draft / submitted / rejected */
    @AutoInject
    private String filter;
    /** 深链目标报价；存在时只返回该报价，由前端保留当前选中对象。 */
    @AutoInject
    private Long quoteId;

    private final TenantContext tenantContext;
    private final QuoteRepository quoteRepository;

    public Page(TenantContext tenantContext, QuoteRepository quoteRepository) {
        this.tenantContext = tenantContext;
        this.quoteRepository = quoteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuoteWorkbenchItem> call(@User Employee employee) {
        Employee operator = tenantContext.requireRole(employee,
                EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Designer);
        Long comId = operator.getCompany().getComId();
        boolean storeManager = operator.getRole() == EmployeeRole.StoreManager;
        boolean storeUnscoped = !storeManager;
        Long storeId = storeManager && operator.getStore() != null
                ? operator.getStore().getStoreId() : null;
        Long designerId = operator.getRole() == EmployeeRole.Designer ? operator.getEmpId() : null;
        int pageIndex = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        PageRequest pageable = PageRequest.of(pageIndex, pageSize);
        if (storeManager && storeId == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String statusFilter = statusFilter();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + normalizedKeyword + "%";
        org.springframework.data.domain.Page<Long> idPage = quoteRepository.findWorkbenchQuoteIds(
                comId, storeUnscoped, storeId, designerId, statusFilter, quoteId,
                normalizedKeyword, pattern, pageable
        );
        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }

        Map<Long, Quote> byId = new LinkedHashMap<>();
        quoteRepository.findWorkbenchQuotes(idPage.getContent())
                .forEach(quote -> byId.put(quote.getQuoteId(), quote));
        LocalDateTime now = LocalDateTime.now();
        List<QuoteWorkbenchItem> items = new ArrayList<>(idPage.getNumberOfElements());
        for (Long id : idPage.getContent()) {
            Quote quote = byId.get(id);
            if (quote != null) items.add(item(quote, now));
        }
        return new PageImpl<>(items, pageable, idPage.getTotalElements());
    }

    private QuoteWorkbenchItem item(Quote quote, LocalDateTime now) {
        Customer customer = quote.getCustomer();
        Property property = quote.getProperty();
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
        item.setLastFeedbackAt(quote.getRejectedAt() != null ? quote.getRejectedAt() : quote.getAcceptedAt());
        item.setAdjustmentCount(Math.max(0, (quote.getVersion() == null ? 1 : quote.getVersion()) - 1));

        LocalDateTime stageStart = statusStart(quote, now);
        long stayDays = Math.max(0, Duration.between(stageStart, now).toDays());
        item.setStayDays(stayDays);
        DesignerWorkbenchRisk.QuoteDecision risk = DesignerWorkbenchRisk.quote(item.getQuoteStatus(), stayDays);
        item.setRiskLevel(risk.riskLevel());
        item.setRiskReason(risk.riskReason());
        item.setNextActionKey(risk.nextActionKey());
        item.setNextActionLabel(risk.nextActionLabel());
        return item;
    }

    private String statusFilter() {
        String value = filter == null || filter.isBlank() ? "all" : filter.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "all" -> null;
            case "draft" -> QuoteStatus.Draft.name();
            case "submitted" -> QuoteStatus.Submitted.name();
            case "rejected" -> QuoteStatus.Rejected.name();
            default -> throw new DefinedException(new DefaultErrorCode("400", "报价筛选条件无效"));
        };
    }

    private LocalDateTime statusStart(Quote quote, LocalDateTime now) {
        if (quote.getStatus() == QuoteStatus.Rejected && quote.getRejectedAt() != null) return quote.getRejectedAt();
        if (quote.getStatus() == QuoteStatus.Submitted && quote.getSubmittedAt() != null) return quote.getSubmittedAt();
        return quote.getCreateTime() == null ? now : quote.getCreateTime().toLocalDateTime();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
