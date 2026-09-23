package com.th4.edge.decoboon.trade.quote;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignLifecycleContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.pack.context.PackageContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceStatus;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.decoboon.quote.context.QuoteContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuoteContextBusinessRulesTest {

    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final QuoteLineRepository lineRepository = mock(QuoteLineRepository.class);
    private final PackageContext packageContext = mock(PackageContext.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final DesignItemRepository designItemRepository = mock(DesignItemRepository.class);
    private final QuoteCalcContext quoteCalcContext = mock(QuoteCalcContext.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final PropertyContext propertyContext = mock(PropertyContext.class);
    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final DesignLifecycleContext designLifecycleContext = mock(DesignLifecycleContext.class);
    private final QuoteContext context = new QuoteContext(
            quoteRepository, lineRepository, packageContext, customerRepository, designItemRepository,
            designLifecycleContext, quoteCalcContext, projectRepository, propertyContext, propertyRepository);

    @Test
    void shouldRejectUnfinishedDesignQuote() {
        Design design = new Design();
        design.setDesignId(3L);
        design.setStatus(DesignStatus.Draft);

        assertThatThrownBy(() -> context.create(design, BigDecimal.ONE))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("设计方案尚未完成");
    }

    @Test
    void shouldOnlyRejectSubmittedQuote() {
        Quote draft = new Quote();
        draft.setQuoteId(4L);
        draft.setStatus(QuoteStatus.Draft);
        when(quoteRepository.findById(4L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> context.reject(4L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("只有已提交报价可以驳回");
    }

    @Test
    void shouldVersionPackageQuoteByCustomerAndTemplate() {
        Company company = new Company();
        company.setComId(1L);
        Customer customer = new Customer();
        customer.setCustId(2L);
        customer.setCompany(company);
        Property property = new Property();
        property.setPropertyId(3L);
        property.setCompany(company);
        property.setCustomer(customer);
        PackageTemplate template = new PackageTemplate();
        template.setTemplateId(4L);
        template.setCompany(company);
        template.setName("品质套餐");
        template.setArea(new BigDecimal("100"));
        template.setPrice(new BigDecimal("100000"));
        template.setDiscount(BigDecimal.ONE);
        Employee employee = new Employee();
        employee.setEmpId(5L);

        when(packageContext.calculateCost(template)).thenReturn(new BigDecimal("60000"));
        when(quoteRepository.countByCompany_ComIdAndCustomer_CustIdAndPackageTemplate_TemplateId(1L, 2L, 4L))
                .thenReturn(2L);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quote quote = context.create(customer, template, property, new BigDecimal("120"), BigDecimal.ONE, employee);

        assertThat(quote.getVersion()).isEqualTo(3);
        assertThat(quote.getMode().name()).isEqualTo("Package");
        verify(quoteRepository).countByCompany_ComIdAndCustomer_CustIdAndPackageTemplate_TemplateId(1L, 2L, 4L);
    }

    @Test
    void shouldBlockSubmitWhenHybridDifferencesArePending() {
        Quote quote = new Quote();
        quote.setQuoteId(4L);
        quote.setStatus(QuoteStatus.Draft);
        QuoteLine line = new QuoteLine();
        line.setDifferenceStatus(QuoteDifferenceStatus.Pending);
        when(quoteRepository.findById(4L)).thenReturn(Optional.of(quote));
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(line));

        assertThatThrownBy(() -> context.submit(4L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("存在待确认的设计差异");
    }

    @Test
    void acceptingQuoteMustAdvanceLinkedDesignToSelected() {
        Company company = new Company();
        company.setComId(1L);
        Customer customer = new Customer();
        customer.setCustId(2L);
        customer.setCompany(company);
        Property property = new Property();
        property.setPropertyId(3L);
        property.setCompany(company);
        property.setCustomer(customer);
        Design design = new Design();
        design.setDesignId(7L);
        design.setStatus(DesignStatus.Ready);
        Quote quote = new Quote();
        quote.setQuoteId(4L);
        quote.setStatus(QuoteStatus.Submitted);
        quote.setCompany(company);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setDesign(design);

        when(quoteRepository.findById(4L)).thenReturn(Optional.of(quote));
        when(propertyRepository.findByPropertyIdAndCompany_ComId(3L, 1L)).thenReturn(Optional.of(property));
        when(customerRepository.findByCustIdAndCompany_ComId(2L, 1L)).thenReturn(Optional.of(customer));
        when(quoteRepository.findByProperty_PropertyIdOrderByQuoteIdDesc(3L)).thenReturn(List.of());
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        context.accept(4L);

        verify(designLifecycleContext).markSelected(design);
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.Accepted);
    }
}
