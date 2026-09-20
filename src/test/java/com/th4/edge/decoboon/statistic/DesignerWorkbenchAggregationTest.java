package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.model.DesignWorkbenchItem;
import com.th4.edge.decoboon.statistic.model.QuoteWorkbenchItem;
import com.th4.edge.decoboon.statistic.model.SignWorkbenchItem;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignerWorkbenchAggregationTest {

    @Test
    void designWorkbenchShouldSplitCustomerPropertiesAndUsePropertyFacts() {
        Employee designer = employee(10L, EmployeeRole.Designer);
        Customer customer = customer(20L, designer);
        Property first = property(101L, customer, designer, PropertyStatus.Measured);
        Property second = property(102L, customer, designer, PropertyStatus.Designing);

        CustomerRepository customers = mock(CustomerRepository.class);
        PropertyRepository properties = mock(PropertyRepository.class);
        DesignRepository designs = mock(DesignRepository.class);
        RequirementRepository requirements = mock(RequirementRepository.class);
        MeasureRepository measures = mock(MeasureRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        when(tenant.requireEmployee(designer)).thenReturn(designer);
        when(customers.findByCompany_ComIdAndDesigner_EmpId(1L, 10L)).thenReturn(List.of(customer));
        when(properties.findByCompany_ComIdAndDesigner_EmpIdOrderByPropertyIdDesc(1L, 10L)).thenReturn(List.of());
        when(properties.findByCompany_ComIdAndCustomer_CustIdIn(1L, List.of(20L))).thenReturn(List.of(first, second));
        when(designs.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(1L, List.of(101L, 102L)))
                .thenReturn(List.of(design(201L, second, DesignStatus.Ready)));
        when(requirements.findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(1L, List.of(101L, 102L)))
                .thenReturn(List.of(requirement(301L, second)));
        Measure completed = measure(401L, first, MeasureStatus.Completed, LocalDateTime.of(2026, 9, 1, 10, 0));
        Measure scheduled = measure(402L, second, MeasureStatus.Scheduled, LocalDateTime.of(2026, 9, 21, 9, 0));
        when(measures.findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(1L, List.of(101L, 102L)))
                .thenReturn(List.of(completed, scheduled));
        when(quotes.findByCompany_ComIdAndProperty_PropertyIdInAndStatusIn(
                1L, List.of(101L, 102L), List.of(QuoteStatus.Accepted, QuoteStatus.Converted)))
                .thenReturn(List.of());

        var provider = new com.th4.edge.decoboon.statistic.service.workbench.design.page.support.Page(
                tenant, customers, designs, requirements, properties, measures, quotes);
        List<DesignWorkbenchItem> result = provider.call(designer);

        assertThat(result).hasSize(2);
        DesignWorkbenchItem firstResult = result.stream().filter(item -> item.getPropertyId().equals(101L)).findFirst().orElseThrow();
        DesignWorkbenchItem secondResult = result.stream().filter(item -> item.getPropertyId().equals(102L)).findFirst().orElseThrow();
        assertThat(firstResult.getDesignStatus()).isEqualTo("Pending");
        assertThat(firstResult.getMeasureAt()).isEqualTo(completed.getMeasureTime());
        assertThat(firstResult.getHasRequirement()).isFalse();
        assertThat(secondResult.getDesignStatus()).isEqualTo("Done");
        assertThat(secondResult.getPendingMeasureAt()).isEqualTo(scheduled.getMeasureTime());
        assertThat(secondResult.getHasRequirement()).isTrue();
        assertThat(secondResult.getSchemeCount()).isEqualTo(1);
    }

    @Test
    void designWorkbenchShouldExcludePropertyAfterQuoteAccepted() {
        Employee designer = employee(10L, EmployeeRole.Designer);
        Customer customer = customer(20L, designer);
        Property property = property(101L, customer, designer, PropertyStatus.Quoted);
        Quote accepted = quote(501L, 2, QuoteStatus.Accepted, customer, property);

        CustomerRepository customers = mock(CustomerRepository.class);
        PropertyRepository properties = mock(PropertyRepository.class);
        DesignRepository designs = mock(DesignRepository.class);
        RequirementRepository requirements = mock(RequirementRepository.class);
        MeasureRepository measures = mock(MeasureRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        when(tenant.requireEmployee(designer)).thenReturn(designer);
        when(customers.findByCompany_ComIdAndDesigner_EmpId(1L, 10L)).thenReturn(List.of(customer));
        when(properties.findByCompany_ComIdAndDesigner_EmpIdOrderByPropertyIdDesc(1L, 10L)).thenReturn(List.of());
        when(properties.findByCompany_ComIdAndCustomer_CustIdIn(1L, List.of(20L))).thenReturn(List.of(property));
        when(quotes.findByCompany_ComIdAndProperty_PropertyIdInAndStatusIn(
                1L, List.of(101L), List.of(QuoteStatus.Accepted, QuoteStatus.Converted)))
                .thenReturn(List.of(accepted));

        var provider = new com.th4.edge.decoboon.statistic.service.workbench.design.page.support.Page(
                tenant, customers, designs, requirements, properties, measures, quotes);

        assertThat(provider.call(designer)).isEmpty();
    }

    @Test
    void quoteWorkbenchShouldUseStatusTimestampsAndPropertyIdentity() {
        Employee designer = employee(10L, EmployeeRole.Designer);
        Customer customer = customer(20L, designer);
        Property property = property(101L, customer, designer, PropertyStatus.Quoted);
        LocalDateTime submittedAt = LocalDateTime.of(2026, 9, 18, 8, 0);
        LocalDateTime rejectedAt = LocalDateTime.of(2026, 9, 19, 9, 0);
        Quote rejected = quote(501L, 2, QuoteStatus.Rejected, customer, property);
        rejected.setSubmittedAt(submittedAt);
        rejected.setRejectedAt(rejectedAt);

        QuoteRepository quotes = mock(QuoteRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        when(tenant.requireEmployee(designer)).thenReturn(designer);
        when(quotes.findByCompany_ComIdAndCreatedBy_EmpIdAndStatusInOrderByQuoteIdDesc(
                1L, 10L, List.of(QuoteStatus.Draft, QuoteStatus.Submitted, QuoteStatus.Rejected)))
                .thenReturn(List.of(rejected));

        var provider = new com.th4.edge.decoboon.statistic.service.workbench.quote.page.support.Page(tenant, quotes);
        List<QuoteWorkbenchItem> result = provider.call(designer);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getPropertyId()).isEqualTo(101L);
            assertThat(item.getSubmittedAt()).isEqualTo(submittedAt);
            assertThat(item.getLastFeedbackAt()).isEqualTo(rejectedAt);
            assertThat(item.getAdjustmentCount()).isEqualTo(1);
        });
    }

    @Test
    void signWorkbenchShouldKeepConvertedQuoteAsSignedProject() {
        Employee designer = employee(10L, EmployeeRole.Designer);
        Customer customer = customer(20L, designer);
        Property property = property(101L, customer, designer, PropertyStatus.Contracted);
        Quote converted = quote(601L, 2, QuoteStatus.Converted, customer, property);
        converted.setAcceptedAt(LocalDateTime.of(2026, 9, 10, 9, 0));
        Project project = new Project();
        project.setProjectId(701L);
        project.setName("客户家装项目");
        project.setStatus(ProjectStatus.Preparing);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(801L);
        contract.setQuote(converted);
        contract.setProject(project);
        contract.setStatus(ContractStatus.Effective);
        contract.setSignedAt(LocalDate.of(2026, 9, 11));

        QuoteRepository quotes = mock(QuoteRepository.class);
        ProjectContractRepository contracts = mock(ProjectContractRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        when(tenant.requireEmployee(designer)).thenReturn(designer);
        when(quotes.findByCompany_ComIdAndCreatedBy_EmpIdAndStatusInOrderByQuoteIdDesc(
                1L, 10L, List.of(QuoteStatus.Accepted, QuoteStatus.Converted)))
                .thenReturn(List.of(converted));
        when(contracts.findByQuote_QuoteIdIn(List.of(601L))).thenReturn(List.of(contract));

        var provider = new com.th4.edge.decoboon.statistic.service.workbench.sign.page.support.Page(
                tenant, quotes, contracts);
        List<SignWorkbenchItem> result = provider.call(designer);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getPropertyId()).isEqualTo(101L);
            assertThat(item.getConverted()).isTrue();
            assertThat(item.getContractStatus()).isEqualTo("Effective");
            assertThat(item.getProjectId()).isEqualTo(701L);
            assertThat(item.getSignedAt()).isEqualTo(LocalDate.of(2026, 9, 11));
        });
    }

    private Employee employee(Long id, EmployeeRole role) {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }

    private Customer customer(Long id, Employee designer) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setCompany(designer.getCompany());
        customer.setDesigner(designer);
        customer.setStatus(CustStatus.Quoted);
        customer.setName("测试客户");
        customer.setTelNo("13800000000");
        return customer;
    }

    private Property property(Long id, Customer customer, Employee designer, PropertyStatus status) {
        Property property = new Property();
        property.setPropertyId(id);
        property.setCompany(customer.getCompany());
        property.setCustomer(customer);
        property.setDesigner(designer);
        property.setStatus(status);
        property.setCommunity("测试小区");
        property.setAddress("测试地址");
        property.setArea(new BigDecimal("118.00"));
        return property;
    }

    private Design design(Long id, Property property, DesignStatus status) {
        Design design = new Design();
        design.setDesignId(id);
        design.setCompany(property.getCompany());
        design.setCustomer(property.getCustomer());
        design.setProperty(property);
        design.setStatus(status);
        return design;
    }

    private Requirement requirement(Long id, Property property) {
        Requirement requirement = new Requirement();
        requirement.setRequirementId(id);
        requirement.setCompany(property.getCompany());
        requirement.setCustomer(property.getCustomer());
        requirement.setProperty(property);
        return requirement;
    }

    private Measure measure(Long id, Property property, MeasureStatus status, LocalDateTime time) {
        Measure measure = new Measure();
        measure.setMeasureId(id);
        measure.setCompany(property.getCompany());
        measure.setCustomer(property.getCustomer());
        measure.setProperty(property);
        measure.setStatus(status);
        measure.setMeasureTime(time);
        return measure;
    }

    private Quote quote(Long id, int version, QuoteStatus status, Customer customer, Property property) {
        Quote quote = new Quote();
        quote.setQuoteId(id);
        quote.setVersion(version);
        quote.setStatus(status);
        quote.setCompany(property.getCompany());
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(customer.getDesigner());
        quote.setAmount(new BigDecimal("100000.00"));
        return quote;
    }
}
