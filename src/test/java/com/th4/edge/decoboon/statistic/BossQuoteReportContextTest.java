package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.configure.TalkStatus;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.model.BossQuoteQuery;
import com.th4.edge.decoboon.statistic.model.BossQuoteReport;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BossQuoteReportContextTest {

    @Test
    void bossQuoteReportShouldAggregateQuotesByCustomerAndKeepPropertyHistory() {
        Company company = company(1L);
        Employee boss = employee(99L, "张伟", EmployeeRole.Boss, company);
        Employee designer = employee(10L, "谢玲玲", EmployeeRole.Designer, company);

        Customer firstCustomer = customer(20L, "王女士", company, designer);
        Property firstProperty = property(101L, "香樟花园 1-101", company, firstCustomer, designer);
        Property secondProperty = property(102L, "香樟花园 2-202", company, firstCustomer, designer);

        Quote firstHistory = quote(401L, company, firstCustomer, firstProperty, designer,
                QuoteStatus.Superseded, "100000", 1);
        Quote firstLatest = quote(402L, company, firstCustomer, firstProperty, designer,
                QuoteStatus.Submitted, "120000", 2);
        firstLatest.setSubmittedAt(LocalDateTime.of(2026, 9, 18, 10, 0));
        Quote accepted = quote(403L, company, firstCustomer, secondProperty, designer,
                QuoteStatus.Accepted, "98000", 1);
        accepted.setAcceptedAt(LocalDateTime.of(2026, 9, 19, 10, 0));
        Talk talk = talk(501L, company, firstCustomer, firstProperty, firstLatest, designer);

        Customer secondCustomer = customer(21L, "李先生", company, null);
        Property thirdProperty = property(103L, "望江府 3-303", company, secondCustomer, null);
        Quote draft = quote(404L, company, secondCustomer, thirdProperty, null,
                QuoteStatus.Draft, "80000", 1);

        CustomerRepository customers = mock(CustomerRepository.class);
        PropertyRepository properties = mock(PropertyRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        TalkRepository talks = mock(TalkRepository.class);
        TenantContext tenant = mock(TenantContext.class);

        when(tenant.requireBoss(boss)).thenReturn(boss);
        when(customers.findByCompany_ComId(1L)).thenReturn(List.of(firstCustomer, secondCustomer));
        when(properties.findByCompany_ComIdAndCustomer_CustIdIn(1L, List.of(20L, 21L)))
                .thenReturn(List.of(firstProperty, secondProperty, thirdProperty));
        when(quotes.findByCompany_ComIdOrderByQuoteIdDesc(1L))
                .thenReturn(List.of(firstLatest, accepted, draft, firstHistory));
        when(talks.findByCompany_ComIdOrderByTalkIdDesc(1L)).thenReturn(List.of(talk));

        var provider = new com.th4.edge.decoboon.statistic.service.boss.quote.report.support.Report(
                tenant, customers, properties, quotes, talks);
        ReflectionTestUtils.setField(provider, "employee", boss);

        BossQuoteReport report = provider.call(new BossQuoteQuery());

        assertThat(report.getCustomerCount()).isEqualTo(2);
        assertThat(report.getActiveCustomerCount()).isEqualTo(2);
        assertThat(report.getPendingConfirmCustomerCount()).isEqualTo(1);
        assertThat(report.getConfirmedCustomerCount()).isEqualTo(1);
        assertThat(report.getTalkGeneratedCustomerCount()).isEqualTo(1);
        assertThat(report.getQuoteCount()).isEqualTo(4);
        assertThat(report.getLatestQuoteAmount()).isEqualByComparingTo("298000");
        assertThat(report.getAcceptedAmount()).isEqualByComparingTo("98000");

        var first = report.getCustomers().stream()
                .filter(item -> item.getCustId().equals(20L))
                .findFirst()
                .orElseThrow();
        assertThat(first.getPropertyCount()).isEqualTo(2);
        assertThat(first.getQuoteCount()).isEqualTo(3);
        assertThat(first.getSubmittedPropertyCount()).isEqualTo(1);
        assertThat(first.getConfirmedPropertyCount()).isEqualTo(1);
        assertThat(first.getLatestQuoteAmount()).isEqualByComparingTo("218000");
        assertThat(first.getDesignerName()).isEqualTo("谢玲玲");
        assertThat(first.getNextAction()).isEqualTo("推动客户确认");
        assertThat(report.getDesignerGroups()).hasSize(2);
        assertThat(report.getDesignerGroups().get(0).getDesignerName()).isEqualTo("待指派设计");
        assertThat(report.getDesignerGroups().get(1).getDesignerName()).isEqualTo("谢玲玲");
        assertThat(report.getDesignerGroups().get(1).getCustomerCount()).isEqualTo(1);

        var firstPropertyReport = first.getProperties().stream()
                .filter(item -> item.getPropertyId().equals(101L))
                .findFirst()
                .orElseThrow();
        assertThat(firstPropertyReport.getQuoteCount()).isEqualTo(2);
        assertThat(firstPropertyReport.getHistory()).extracting("version").containsExactly(2, 1);
        assertThat(firstPropertyReport.getLatestQuoteStatus()).isEqualTo("Submitted");
        assertThat(firstPropertyReport.getTalkCount()).isEqualTo(1);
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setComId(id);
        return company;
    }

    private Employee employee(Long id, String name, EmployeeRole role, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setName(name);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setCompany(company);
        return employee;
    }

    private Customer customer(Long id, String name, Company company, Employee designer) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setName(name);
        customer.setTelNo("19900000000");
        customer.setCompany(company);
        customer.setDesigner(designer);
        customer.setStatus(CustStatus.Quoted);
        customer.setCommunity("测试小区");
        customer.setArea(new BigDecimal("118"));
        return customer;
    }

    private Property property(Long id, String address, Company company, Customer customer, Employee designer) {
        Property property = new Property();
        property.setPropertyId(id);
        property.setPropertyNo("P" + id);
        property.setCompany(company);
        property.setCustomer(customer);
        property.setDesigner(designer);
        property.setAddress(address);
        property.setCommunity(address.split(" ")[0]);
        property.setArea(new BigDecimal("118"));
        return property;
    }

    private Quote quote(Long id, Company company, Customer customer, Property property, Employee owner,
                        QuoteStatus status, String amount, int version) {
        Quote quote = new Quote();
        quote.setQuoteId(id);
        quote.setNo("Q" + id);
        quote.setVersion(version);
        quote.setCompany(company);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(owner);
        quote.setStatus(status);
        quote.setAmount(new BigDecimal(amount));
        return quote;
    }

    private Talk talk(Long id, Company company, Customer customer, Property property, Quote quote, Employee owner) {
        Talk talk = new Talk();
        talk.setTalkId(id);
        talk.setNo("T" + id);
        talk.setCompany(company);
        talk.setCustomer(customer);
        talk.setProperty(property);
        talk.setQuote(quote);
        talk.setDesign(quote.getDesign());
        talk.setOwner(owner);
        talk.setStatus(TalkStatus.Generated);
        talk.setBuiltAt(LocalDateTime.of(2026, 9, 20, 10, 0));
        return talk;
    }
}
