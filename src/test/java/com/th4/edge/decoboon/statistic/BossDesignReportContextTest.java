package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.configure.TalkStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.model.BossDesignQuery;
import com.th4.edge.decoboon.statistic.model.BossDesignReport;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BossDesignReportContextTest {

    @Test
    void bossShouldSeeDesignerWorkContentQuoteTalkAndCustomerConfirmation() {
        Company company = company(1L);
        Employee boss = employee(99L, "张伟", EmployeeRole.Boss, company);
        Employee xie = employee(10L, "谢玲玲", EmployeeRole.Designer, company);
        Employee dong = employee(11L, "董夏鹏", EmployeeRole.Designer, company);

        Customer designing = customer(20L, "设计客户", company, xie, CustStatus.Quoted);
        Property designingProperty = property(101L, "香樟花园 1-101", company, designing, xie);
        Design design = design(201L, "现代简约方案", company, designing, designingProperty, xie, DesignStatus.Draft);
        Requirement requirement = requirement(301L, designing, designingProperty, xie);
        Quote submittedQuote = quote(401L, company, designing, designingProperty, design, QuoteStatus.Submitted, "128000");
        Talk talk = talk(501L, company, designing, designingProperty, design, submittedQuote);

        Customer confirmed = customer(21L, "已确认客户", company, dong, CustStatus.Signed);
        Property confirmedProperty = property(102L, "望江府 3-202", company, confirmed, dong);
        Quote confirmedQuote = quote(402L, company, confirmed, confirmedProperty, null, QuoteStatus.Accepted, "98000");

        Customer unassigned = customer(22L, "待派客户", company, null, CustStatus.PendingMeasure);
        Property unassignedProperty = property(103L, "锦绣园 2-303", company, unassigned, null);

        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        DesignRepository designRepository = mock(DesignRepository.class);
        RequirementRepository requirementRepository = mock(RequirementRepository.class);
        SpaceRepository spaceRepository = mock(SpaceRepository.class);
        DesignItemRepository designItemRepository = mock(DesignItemRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        TalkRepository talkRepository = mock(TalkRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        TenantContext tenantContext = mock(TenantContext.class);

        when(tenantContext.requireBoss(boss)).thenReturn(boss);
        when(customerRepository.findByCompany_ComId(1L)).thenReturn(List.of(designing, confirmed, unassigned));
        when(propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(1L, List.of(20L, 21L, 22L)))
                .thenReturn(List.of(designingProperty, confirmedProperty, unassignedProperty));
        when(designRepository.findByCompany_ComIdOrderByDesignIdDesc(1L)).thenReturn(List.of(design));
        when(requirementRepository.findByCompany_ComIdOrderByRequirementIdDesc(1L)).thenReturn(List.of(requirement));
        when(quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(1L)).thenReturn(List.of(confirmedQuote, submittedQuote));
        when(talkRepository.findByCompany_ComIdOrderByTalkIdDesc(1L)).thenReturn(List.of(talk));
        when(spaceRepository.findByDesign_DesignIdIn(java.util.Set.of(201L))).thenReturn(List.of(space(601L, design)));
        when(designItemRepository.findByDesign_DesignIdIn(java.util.Set.of(201L))).thenReturn(List.of(item(701L, design)));
        when(measureRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(
                1L, List.of(101L, 102L, 103L)))
                .thenReturn(List.of(
                        measure(801L, designingProperty, MeasureStatus.Completed, LocalDateTime.of(2026, 9, 1, 10, 0)),
                        measure(802L, unassignedProperty, MeasureStatus.Completed, LocalDateTime.of(2026, 9, 2, 10, 0))));
        when(employeeRepository.findByCompany_ComIdAndRoleOrderByName(1L, EmployeeRole.Designer))
                .thenReturn(List.of(xie, dong));

        var provider = new com.th4.edge.decoboon.statistic.service.boss.design.report.support.Report(
                tenantContext, customerRepository, propertyRepository, designRepository, requirementRepository,
                spaceRepository, designItemRepository, quoteRepository, talkRepository, measureRepository, employeeRepository);
        ReflectionTestUtils.setField(provider, "employee", boss);

        BossDesignReport report = provider.call(new BossDesignQuery());

        assertThat(report.getPipelineCount()).isEqualTo(2);
        assertThat(report.getUnassignedCount()).isEqualTo(1);
        assertThat(report.getDesigningCount()).isEqualTo(1);
        assertThat(report.getQuoteSubmittedCount()).isEqualTo(1);
        assertThat(report.getConfirmedCount()).isEqualTo(1);
        assertThat(report.getTalkGeneratedCount()).isEqualTo(1);
        assertThat(report.getQuoteAmount()).isEqualByComparingTo("226000");
        assertThat(report.getAcceptedAmount()).isEqualByComparingTo("98000");
        assertThat(report.getDesigners()).extracting("designerName")
                .containsExactlyInAnyOrder("谢玲玲", "董夏鹏");

        var first = report.getItems().stream()
                .filter(item -> item.getPropertyId().equals(101L))
                .findFirst()
                .orElseThrow();
        assertThat(first.getDesignerName()).isEqualTo("谢玲玲");
        assertThat(first.getStage()).isEqualTo("QuoteSubmitted");
        assertThat(first.getLatestRequirementTitle()).isEqualTo("客厅收纳需求");
        assertThat(first.getLatestSchemeName()).isEqualTo("现代简约方案");
        assertThat(first.getLatestSchemeSpaceCount()).isEqualTo(1);
        assertThat(first.getLatestSchemeItemCount()).isEqualTo(1);
        assertThat(first.getLatestQuoteAmount()).isEqualByComparingTo("128000");
        assertThat(first.getLatestTalkStatus()).isEqualTo("Generated");
        assertThat(first.getCustomerConfirmed()).isFalse();

        var second = report.getItems().stream()
                .filter(item -> item.getPropertyId().equals(102L))
                .findFirst()
                .orElseThrow();
        assertThat(second.getStage()).isEqualTo("Confirmed");
        assertThat(second.getCustomerConfirmed()).isTrue();
        assertThat(second.getCustomerConfirmedAt()).isNotNull();
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

    private Customer customer(Long id, String name, Company company, Employee designer, CustStatus status) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setName(name);
        customer.setTelNo("19900000000");
        customer.setCompany(company);
        customer.setDesigner(designer);
        customer.setStatus(status);
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

    private Design design(Long id, String name, Company company, Customer customer, Property property,
                          Employee owner, DesignStatus status) {
        Design design = new Design();
        design.setDesignId(id);
        design.setNo("D" + id);
        design.setName(name);
        design.setCompany(company);
        design.setCustomer(customer);
        design.setProperty(property);
        design.setOwner(owner);
        design.setStatus(status);
        return design;
    }

    private Requirement requirement(Long id, Customer customer, Property property, Employee owner) {
        Requirement requirement = new Requirement();
        requirement.setRequirementId(id);
        requirement.setCustomer(customer);
        requirement.setProperty(property);
        requirement.setOwner(owner);
        requirement.setTitle("客厅收纳需求");
        return requirement;
    }

    private Quote quote(Long id, Company company, Customer customer, Property property, Design design,
                        QuoteStatus status, String amount) {
        Quote quote = new Quote();
        quote.setQuoteId(id);
        quote.setNo("Q" + id);
        quote.setVersion(1);
        quote.setCompany(company);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setDesign(design);
        quote.setStatus(status);
        quote.setAmount(new BigDecimal(amount));
        if (status == QuoteStatus.Submitted) {
            quote.setSubmittedAt(LocalDateTime.of(2026, 9, 10, 10, 0));
        }
        if (status == QuoteStatus.Accepted || status == QuoteStatus.Converted) {
            quote.setAcceptedAt(LocalDateTime.of(2026, 9, 11, 10, 0));
        }
        return quote;
    }

    private Talk talk(Long id, Company company, Customer customer, Property property, Design design, Quote quote) {
        Talk talk = new Talk();
        talk.setTalkId(id);
        talk.setNo("T" + id);
        talk.setCompany(company);
        talk.setCustomer(customer);
        talk.setProperty(property);
        talk.setDesign(design);
        talk.setQuote(quote);
        talk.setOwner(design.getOwner());
        talk.setStatus(TalkStatus.Generated);
        talk.setBuiltAt(LocalDateTime.of(2026, 9, 12, 10, 0));
        return talk;
    }

    private Space space(Long id, Design design) {
        Space space = new Space();
        space.setSpaceId(id);
        space.setDesign(design);
        return space;
    }

    private DesignItem item(Long id, Design design) {
        DesignItem item = new DesignItem();
        item.setItemId(id);
        item.setDesign(design);
        return item;
    }

    private Measure measure(Long id, Property property, MeasureStatus status, LocalDateTime time) {
        Measure measure = new Measure();
        measure.setMeasureId(id);
        measure.setProperty(property);
        measure.setStatus(status);
        measure.setMeasureTime(time);
        return measure;
    }
}
