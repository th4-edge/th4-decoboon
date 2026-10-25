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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
        assertThat(tasks.get(0).getStateLabel()).isEqualTo("预约逾期");
        assertThat(tasks.get(0).getRiskLevel()).isEqualTo("high");
        // 逾期主行动＝改约（用户裁决 2026-10-12）。
        assertThat(tasks.get(0).getMainActionKey()).isEqualTo("reschedule_measure");
        assertThat(tasks.get(0).getMainActionLabel()).isEqualTo("联系客户改约");
        // 「登记量房结论」降级为并列动作，但必须真实可达：覆盖已上门未登记的场景。
        assertThat(tasks.get(0).getSecondaryActionKey()).isEqualTo("complete_measure");
        assertThat(tasks.get(0).getSecondaryActionLabel()).isEqualTo("登记量房结论");
        assertThat(tasks.get(0).getNextActionKey()).isEqualTo("measure/complete");
        assertThat(tasks.get(0).getPriority()).isEqualTo(1);
    }

    @Test
    void shouldAssembleOnlyCurrentMeasurePageAnchors() {
        ContextFixture fixture = context();
        Company company = company(10L);
        Employee designer = employee(20L, company);
        Customer customer = customer(100L, company, CustStatus.PendingMeasure);
        Property property = property(200L, company, customer, PropertyStatus.PendingMeasure);
        PropertyRepository.MeasureTaskAnchor anchor = mock(PropertyRepository.MeasureTaskAnchor.class);
        when(anchor.getTaskType()).thenReturn("property");
        when(anchor.getCustomerId()).thenReturn(100L);
        when(anchor.getPropertyId()).thenReturn(200L);
        when(anchor.getStateKey()).thenReturn("not_scheduled");
        when(fixture.propertyRepository().findMeasureTaskAnchors(
                eq(10L), eq(20L), eq(false), eq("林"), eq("notScheduled"),
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(anchor), PageRequest.of(0, 20), 21));
        when(fixture.customerRepository().findByCompany_ComIdAndCustIdIn(10L, List.of(100L)))
                .thenReturn(List.of(customer));
        when(fixture.propertyRepository().findByCompany_ComIdAndPropertyIdIn(10L, List.of(200L)))
                .thenReturn(List.of(property));
        when(fixture.measureRepository().findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(10L, List.of(200L)))
                .thenReturn(List.of());
        when(fixture.requirementRepository().findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(10L, List.of(200L)))
                .thenReturn(List.of());
        when(fixture.designRepository().findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(10L, List.of(200L)))
                .thenReturn(List.of());

        DesignerMeasureQuery query = new DesignerMeasureQuery();
        query.setCurrent(1);
        query.setSize(20);
        query.setKeyword("林");
        query.setState("notScheduled");
        Page<DesignerMeasureTask> page = fixture.context().page(designer, query);

        assertThat(page.getTotalElements()).isEqualTo(21);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getMainActionLabel()).isEqualTo("预约量房");
    }

    @Test
    void shouldRecommendAppointmentWhenMeasureHasNoSchedule() {
        ContextFixture fixture = context();
        Company company = company(10L);
        Employee designer = employee(20L, company);
        Customer customer = customer(100L, company, CustStatus.PendingMeasure);
        Property property = property(200L, company, customer, PropertyStatus.PendingMeasure);

        stubTaskLookup(fixture, company, designer, customer, property, null);

        DesignerMeasureTask task = fixture.context().list(designer, new DesignerMeasureQuery()).get(0);
        assertThat(task.getStateKey()).isEqualTo("not_scheduled");
        assertThat(task.getMainActionKey()).isEqualTo("schedule_measure");
        assertThat(task.getMainActionLabel()).isEqualTo("预约量房");
        assertThat(task.getNextActionKey()).isEqualTo("measure/schedule");
        assertThat(task.getNextActionLabel()).isEqualTo("约定量房时间");
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
        assertThat(task.getScheduleStateKey()).isEqualTo("scheduled");
        assertThat(task.getScheduledAt()).isNotNull();
    }

    @Test
    void shouldMarkCompletedMeasureWithoutSchedule() {
        ContextFixture fixture = context();
        Company company = company(10L);
        Employee designer = employee(20L, company);
        Customer customer = customer(100L, company, CustStatus.PendingMeasure);
        Property property = property(200L, company, customer, PropertyStatus.Measured);
        LocalDateTime completedAt = LocalDateTime.now().minusDays(2);

        stubTaskLookup(fixture, company, designer, customer, property,
                measure(301L, property, MeasureStatus.Completed, null, completedAt));

        DesignerMeasureTask task = fixture.context().list(designer, new DesignerMeasureQuery()).get(0);
        assertThat(task.getScheduleStateKey()).isEqualTo("completed_without_schedule");
        assertThat(task.getScheduleStateName()).isEqualTo("未预约（直接登记量房）");
        assertThat(task.getScheduledAt()).isNull();
    }

    @Test
    void shouldMarkCompletedMeasureWithInvalidScheduleHistory() {
        ContextFixture fixture = context();
        Company company = company(10L);
        Employee designer = employee(20L, company);
        Customer customer = customer(100L, company, CustStatus.PendingMeasure);
        Property property = property(200L, company, customer, PropertyStatus.Measured);
        LocalDateTime completedAt = LocalDateTime.now().minusDays(2);
        LocalDateTime invalidScheduledAt = completedAt.plusHours(3);

        stubTaskLookup(fixture, company, designer, customer, property,
                measure(301L, property, MeasureStatus.Completed, invalidScheduledAt, completedAt));

        DesignerMeasureTask task = fixture.context().list(designer, new DesignerMeasureQuery()).get(0);
        assertThat(task.getScheduleStateKey()).isEqualTo("invalid_history");
        assertThat(task.getScheduleStateName()).isEqualTo("预约记录异常，暂不可确认");
        assertThat(task.getScheduledAt()).isNull();
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

    @Test
    void shouldKeepRecordMeasureActionWhenAppointmentNotOverdue() {
        ContextFixture fixture = context();
        Company company = company(10L);
        Employee designer = employee(20L, company);
        Customer customer = customer(100L, company, CustStatus.PendingMeasure);
        Property property = property(200L, company, customer, PropertyStatus.PendingMeasure);

        stubTaskLookup(fixture, company, designer, customer, property,
                measure(302L, property, MeasureStatus.Scheduled, LocalDateTime.now().plusHours(2), null));

        DesignerMeasureTask task = fixture.context().list(designer, new DesignerMeasureQuery()).get(0);
        assertThat(task.getStateKey()).isEqualTo("scheduled");
        assertThat(task.getMainActionKey()).isEqualTo("complete_measure");
        assertThat(task.getNextActionKey()).isEqualTo("measure/complete");
        // 未逾期没有并列动作：改约由中栏「调整预约时间」承担。
        assertThat(task.getSecondaryActionKey()).isNull();
    }

    @Test
    void shouldKeepRecordMeasureActionWhenAppointmentOverdue() {
        ContextFixture fixture = context();
        Company company = company(10L);
        Employee designer = employee(20L, company);
        Customer customer = customer(100L, company, CustStatus.PendingMeasure);
        Property property = property(200L, company, customer, PropertyStatus.PendingMeasure);

        stubTaskLookup(fixture, company, designer, customer, property,
                measure(303L, property, MeasureStatus.Scheduled, LocalDateTime.now().minusHours(6), null));

        DesignerMeasureTask task = fixture.context().list(designer, new DesignerMeasureQuery()).get(0);
        assertThat(task.getStateKey()).isEqualTo("overdue");
        assertThat(task.getMainActionKey()).isEqualTo("reschedule_measure");
        assertThat(task.getMainActionLabel()).isEqualTo("联系客户改约");
        assertThat(task.getSecondaryActionKey()).isEqualTo("complete_measure");
        assertThat(task.getSecondaryActionLabel()).isEqualTo("登记量房结论");
    }

    private ContextFixture context() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        RequirementRepository requirementRepository = mock(RequirementRepository.class);
        DesignRepository designRepository = mock(DesignRepository.class);
        return new ContextFixture(
                new DesignerMeasureTaskContext(customerRepository, propertyRepository, measureRepository,
                        requirementRepository, designRepository),
                customerRepository,
                propertyRepository,
                measureRepository,
                requirementRepository,
                designRepository);
    }

    private void stubTaskLookup(ContextFixture fixture,
                                Company company,
                                Employee designer,
                                Customer customer,
                                Property property,
                                Measure measure) {
        when(fixture.customerRepository().findByCompany_ComIdAndOwner_EmpId(company.getComId(), designer.getEmpId()))
                .thenReturn(List.of(customer));
        when(fixture.customerRepository().findByCompany_ComIdAndDesigner_EmpId(company.getComId(), designer.getEmpId()))
                .thenReturn(List.of());
        when(fixture.propertyRepository().findByCompany_ComIdAndCustomer_CustIdIn(company.getComId(), List.of(customer.getCustId())))
                .thenReturn(List.of(property));
        when(fixture.measureRepository().findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(anyLong(), any()))
                .thenReturn(measure == null ? List.of() : List.of(measure));
        when(fixture.requirementRepository().findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(anyLong(), any()))
                .thenReturn(List.of());
        when(fixture.designRepository().findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(anyLong(), any()))
                .thenReturn(List.of());
    }

    private record ContextFixture(DesignerMeasureTaskContext context,
                                  CustomerRepository customerRepository,
                                  PropertyRepository propertyRepository,
                                  MeasureRepository measureRepository,
                                  RequirementRepository requirementRepository,
                                  DesignRepository designRepository) {
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
