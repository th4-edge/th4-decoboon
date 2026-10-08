package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.statistic.context.DesignerMeasureTaskContext;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureQuery;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureTask;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignerMeasureTaskContextTest {

    @Test
    void shouldBuildPropertyAnchoredTasksAndServerActions() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        RequirementRepository requirementRepository = mock(RequirementRepository.class);
        DesignRepository designRepository = mock(DesignRepository.class);
        DesignerMeasureTaskContext context = new DesignerMeasureTaskContext(
                customerRepository, propertyRepository, measureRepository, requirementRepository, designRepository);

        Company company = company(10L);
        Employee designer = employee(20L, company);
        Customer customer = customer(100L, company, CustStatus.PendingMeasure);
        Property property = property(200L, company, customer, PropertyStatus.PendingMeasure);

        when(customerRepository.findByCompany_ComIdAndOwner_EmpId(10L, 20L)).thenReturn(List.of(customer));
        when(customerRepository.findByCompany_ComIdAndDesigner_EmpId(10L, 20L)).thenReturn(List.of());
        when(propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(10L, List.of(100L)))
                .thenReturn(List.of(property));

        LocalDateTime now = LocalDateTime.now();
        Measure overdue = measure(300L, property, MeasureStatus.Scheduled, now.minusHours(5), null);
        when(measureRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(anyLong(), any()))
                .thenReturn(List.of(overdue));
        when(requirementRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(anyLong(), any()))
                .thenReturn(List.of());
        when(designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(anyLong(), any()))
                .thenReturn(List.of());

        List<DesignerMeasureTask> tasks = context.list(designer, new DesignerMeasureQuery());
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getPropertyId()).isEqualTo(200L);
        assertThat(tasks.get(0).getStateKey()).isEqualTo("overdue");
        assertThat(tasks.get(0).getMainActionKey()).isEqualTo("reschedule_measure");
    }

    @Test
    void shouldKeepCompletedMeasureUntilDesignExists() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        RequirementRepository requirementRepository = mock(RequirementRepository.class);
        DesignRepository designRepository = mock(DesignRepository.class);
        DesignerMeasureTaskContext context = new DesignerMeasureTaskContext(
                customerRepository, propertyRepository, measureRepository, requirementRepository, designRepository);

        Company company = company(10L);
        Employee designer = employee(20L, company);
        Customer customer = customer(100L, company, CustStatus.Quoted);
        Property property = property(200L, company, customer, PropertyStatus.Measured);

        when(customerRepository.findByCompany_ComIdAndOwner_EmpId(10L, 20L)).thenReturn(List.of(customer));
        when(customerRepository.findByCompany_ComIdAndDesigner_EmpId(10L, 20L)).thenReturn(List.of());
        when(propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(10L, List.of(100L)))
                .thenReturn(List.of(property));
        when(measureRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(anyLong(), any()))
                .thenReturn(List.of(measure(301L, property, MeasureStatus.Completed,
                        LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(3))));
        when(requirementRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(anyLong(), any()))
                .thenReturn(List.of());
        when(designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(anyLong(), any()))
                .thenReturn(List.of());

        DesignerMeasureTask task = context.list(designer, new DesignerMeasureQuery()).get(0);
        assertThat(task.getStateKey()).isEqualTo("completed_pending_handoff");
        assertThat(task.getMainActionKey()).isEqualTo("create_scheme");
    }

    @Test
    void shouldRemoveCompletedMeasureAfterDesignStarts() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        RequirementRepository requirementRepository = mock(RequirementRepository.class);
        DesignRepository designRepository = mock(DesignRepository.class);
        DesignerMeasureTaskContext context = new DesignerMeasureTaskContext(
                customerRepository, propertyRepository, measureRepository, requirementRepository, designRepository);

        Company company = company(10L);
        Employee designer = employee(20L, company);
        Customer customer = customer(100L, company, CustStatus.Quoted);
        Property property = property(200L, company, customer, PropertyStatus.Measured);
        Design design = new Design();
        design.setDesignId(400L);
        design.setProperty(property);

        when(customerRepository.findByCompany_ComIdAndOwner_EmpId(10L, 20L)).thenReturn(List.of(customer));
        when(customerRepository.findByCompany_ComIdAndDesigner_EmpId(10L, 20L)).thenReturn(List.of());
        when(propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(10L, List.of(100L)))
                .thenReturn(List.of(property));
        when(measureRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(anyLong(), any()))
                .thenReturn(List.of(measure(301L, property, MeasureStatus.Completed,
                        LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(3))));
        when(requirementRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(anyLong(), any()))
                .thenReturn(List.of());
        when(designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(anyLong(), any()))
                .thenReturn(List.of(design));

        assertThat(context.list(designer, new DesignerMeasureQuery())).isEmpty();
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setComId(id);
        return company;
    }

    private Employee employee(Long id, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setCompany(company);
        return employee;
    }

    private Customer customer(Long id, Company company, CustStatus status) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setCompany(company);
        customer.setStatus(status);
        customer.setName("客户" + id);
        return customer;
    }

    private Property property(Long id, Company company, Customer customer, PropertyStatus status) {
        Property property = new Property();
        property.setPropertyId(id);
        property.setCompany(company);
        property.setCustomer(customer);
        property.setStatus(status);
        property.setCommunity("测试小区");
        return property;
    }

    private Measure measure(Long id, Property property, MeasureStatus status,
                            LocalDateTime scheduledAt, LocalDateTime completedAt) {
        Measure measure = new Measure();
        measure.setMeasureId(id);
        measure.setCompany(property.getCompany());
        measure.setCustomer(property.getCustomer());
        measure.setProperty(property);
        measure.setStatus(status);
        measure.setScheduledAt(scheduledAt);
        measure.setCompletedAt(completedAt);
        return measure;
    }
}
