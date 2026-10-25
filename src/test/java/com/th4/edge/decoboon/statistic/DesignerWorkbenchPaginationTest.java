package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignerWorkbenchPaginationTest {

   @Test
    void quoteWorkbenchShouldConsumeServerPageWithoutLoadingAllCandidates() {
        Employee designer = employee(10L);
        Customer activeCustomer = customer(20L, designer, CustStatus.Quoted);
        Property activeProperty = property(101L, activeCustomer, designer);

        List<Quote> candidates = new ArrayList<>();
        for (int index = 1; index <= 205; index++) {
            QuoteStatus status = switch (index % 3) {
                case 1 -> QuoteStatus.Draft;
                case 2 -> QuoteStatus.Submitted;
                default -> QuoteStatus.Rejected;
            };
            candidates.add(quote(index, status, activeCustomer, activeProperty));
        }

        QuoteRepository quotes = mock(QuoteRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        when(tenant.requireRole(eq(designer), any(EmployeeRole[].class))).thenReturn(designer);
        when(quotes.findWorkbenchQuotes(anyCollection())).thenAnswer(invocation -> {
            List<Long> ids = new ArrayList<>(invocation.getArgument(0));
            return candidates.stream().filter(item -> ids.contains(item.getQuoteId())).toList();
        });
        when(quotes.findWorkbenchQuoteIds(
                eq(1L), eq(true), isNull(), eq(10L), isNull(), isNull(), eq(""), eq("%%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        candidates.subList(20, 40).stream().map(Quote::getQuoteId).toList(),
                        PageRequest.of(1, 20), candidates.size()));
        List<Quote> drafts = candidates.stream().filter(item -> item.getStatus() == QuoteStatus.Draft).toList();
        when(quotes.findWorkbenchQuoteIds(
                eq(1L), eq(true), isNull(), eq(10L), eq("Draft"), isNull(), eq(""), eq("%%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        drafts.subList(0, 20).stream().map(Quote::getQuoteId).toList(),
                        PageRequest.of(0, 20), drafts.size()));
        when(quotes.findWorkbenchQuoteIds(
                eq(1L), eq(true), isNull(), eq(10L), isNull(), isNull(), eq("q0205"), eq("%q0205%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(205L), PageRequest.of(0, 20), 1));

        var provider = new com.th4.edge.decoboon.statistic.service.workbench.quote.page.support.Page(tenant, quotes);
        ReflectionTestUtils.setField(provider, "current", 2);
        ReflectionTestUtils.setField(provider, "size", 20);
        ReflectionTestUtils.setField(provider, "filter", "all");
        var page = provider.call(designer);

        assertThat(page.getTotalElements()).isEqualTo(205);
        assertThat(page.getTotalPages()).isEqualTo(11);
        assertThat(page.getContent()).hasSize(20);

        ReflectionTestUtils.setField(provider, "current", 1);
        ReflectionTestUtils.setField(provider, "filter", "draft");
        var draftPage = provider.call(designer);
        assertThat(draftPage.getTotalElements()).isEqualTo(drafts.size());
        assertThat(draftPage.getContent()).allMatch(item -> "Draft".equals(item.getQuoteStatus()));

        ReflectionTestUtils.setField(provider, "filter", "all");
        ReflectionTestUtils.setField(provider, "keyword", "Q0205");
        var searched = provider.call(designer);
        assertThat(searched.getTotalElements()).isEqualTo(1);
        assertThat(searched.getContent()).singleElement()
                .satisfies(item -> assertThat(item.getQuoteNo()).isEqualTo("Q0205"));
    }

    @Test
    void signWorkbenchShouldPage205RowsAndExcludeDormant() {
        Employee designer = employee(10L);
        Customer activeCustomer = customer(20L, designer, CustStatus.Signed);
        Customer dormantCustomer = customer(21L, designer, CustStatus.Dormant);
        Property activeProperty = property(101L, activeCustomer, designer);
        Property dormantProperty = property(102L, dormantCustomer, designer);

        List<Quote> candidates = new ArrayList<>();
        for (int index = 1; index <= 205; index++) {
            Quote quote = quote(index, QuoteStatus.Accepted, activeCustomer, activeProperty);
            quote.setAcceptedAt(LocalDateTime.now().minusDays(5));
            candidates.add(quote);
        }
        Quote dormant = quote(900L, QuoteStatus.Accepted, dormantCustomer, dormantProperty);
        dormant.setAcceptedAt(LocalDateTime.now().minusDays(5));
        candidates.add(dormant);
        List<Quote> activeCandidates = candidates.subList(0, 205);

        QuoteRepository quotes = mock(QuoteRepository.class);
        ProjectContractRepository contracts = mock(ProjectContractRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        when(tenant.requireEmployee(designer)).thenReturn(designer);
        when(tenant.requireRole(eq(designer), any(EmployeeRole[].class))).thenReturn(designer);
        when(quotes.findWorkbenchSignQuoteIds(
                eq(1L), eq(false), isNull(), isNull(), eq(10L), eq("toCreate"), isNull(), eq(""), eq("%%"), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable request = invocation.getArgument(9);
                    int from = Math.min((int) request.getOffset(), activeCandidates.size());
                    int to = Math.min(from + request.getPageSize(), activeCandidates.size());
                    return new PageImpl<>(
                            activeCandidates.subList(from, to).stream().map(Quote::getQuoteId).toList(),
                            request, activeCandidates.size());
                });
        when(quotes.findSignWorkbenchQuotes(anyCollection())).thenAnswer(invocation -> {
            List<Long> ids = new ArrayList<>(invocation.getArgument(0));
            return candidates.stream().filter(item -> ids.contains(item.getQuoteId())).toList();
        });
        when(contracts.findWorkbenchByQuoteIds(anyCollection())).thenReturn(List.of());

        var provider = new com.th4.edge.decoboon.statistic.service.workbench.sign.page.support.Page(
                tenant, quotes, contracts);
        ReflectionTestUtils.setField(provider, "current", 1);
        ReflectionTestUtils.setField(provider, "size", 20);
        ReflectionTestUtils.setField(provider, "filter", "toCreate");
        var first = provider.call(designer);

        assertThat(first.getTotalElements()).isEqualTo(205);
        assertThat(first.getTotalPages()).isEqualTo(11);
        assertThat(first.getContent()).hasSize(20);
        assertThat(first.getContent()).noneMatch(item -> item.getCustId().equals(21L));
        assertThat(first.getContent()).allMatch(item -> item.getContractId() == null);

        ReflectionTestUtils.setField(provider, "current", 11);
        assertThat(provider.call(designer).getContent()).hasSize(5);
    }

    private Employee employee(Long id) {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(EmployeeRole.Designer);
        employee.setStatus(com.th4.edge.tenant.user.configure.EmployeeStatus.Active);
        employee.setCompany(company);
        return employee;
    }

    private Customer customer(Long id, Employee designer, CustStatus status) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setCompany(designer.getCompany());
        customer.setDesigner(designer);
        customer.setStatus(status);
        customer.setName(status == CustStatus.Dormant ? "休眠客户" : "分页客户");
        customer.setTelNo("13800000000");
        return customer;
    }

    private Property property(Long id, Customer customer, Employee designer) {
        Property property = new Property();
        property.setPropertyId(id);
        property.setCompany(customer.getCompany());
        property.setCustomer(customer);
        property.setDesigner(designer);
        property.setStatus(PropertyStatus.Quoted);
        property.setCommunity("分页测试小区");
        property.setAddress("分页测试地址");
        property.setArea(new BigDecimal("100.00"));
        return property;
    }

    private Quote quote(long id, QuoteStatus status, Customer customer, Property property) {
        Quote quote = new Quote();
        quote.setQuoteId(id);
        quote.setNo(String.format("Q%04d", id));
        quote.setVersion(1);
        quote.setAmount(new BigDecimal("1000.00"));
        quote.setStatus(status);
        quote.setCompany(customer.getCompany());
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(customer.getDesigner());
        return quote;
    }
}
