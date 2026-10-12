package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.service.cust.handoff.support.Handoff;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HandoffDesignerChangeTest {

    @Test
    void bossShouldReplaceCustomerDesignerWithAnyActiveEmployeeAcrossActiveFacts() {
        Company company = company(1L);
        Employee boss = employee(99L, "张伟", EmployeeRole.Boss, company);
        Employee previous = employee(10L, "谢玲玲", EmployeeRole.Designer, company);
        Employee next = employee(11L, "董夏鹏", EmployeeRole.Marketer, company);

        Customer customer = new Customer();
        customer.setCustId(20L);
        customer.setName("王女士");
        customer.setCompany(company);
        customer.setStatus(CustStatus.Following);
        customer.setDesigner(previous);

        Property first = property(101L, customer, previous);
        Property second = property(102L, customer, previous);
        Measure scheduled = measure(201L, first, previous, MeasureStatus.Scheduled);
        Measure completed = measure(202L, first, previous, MeasureStatus.Completed);
        Design active = design(301L, customer, first, previous, DesignStatus.Draft);
        Design archived = design(302L, customer, first, previous, DesignStatus.Archived);
        Requirement requirement = requirement(401L, customer, first, previous);

        CustomerRepository customers = mock(CustomerRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        DataScopeContext dataScope = mock(DataScopeContext.class);
        NoticeContext notices = mock(NoticeContext.class);
        PropertyContext properties = mock(PropertyContext.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        MeasureRepository measures = mock(MeasureRepository.class);
        DesignRepository designs = mock(DesignRepository.class);
        RequirementRepository requirements = mock(RequirementRepository.class);

        when(dataScope.requireEmployee(boss)).thenReturn(boss);
        when(dataScope.resolve(boss)).thenReturn(new DataScope(DataScopeType.Company, 1L, null, null, 99L));
        when(customers.findOne(any(Specification.class))).thenReturn(Optional.of(customer));
        when(employees.findByEmpIdAndCompany_ComId(11L, 1L)).thenReturn(next);
        when(properties.resolve(101L, customer, boss)).thenReturn(first);
        when(propertyRepository.findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(1L, 20L))
                .thenReturn(List.of(first, second));
        when(properties.assignDesigner(any(), any())).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);
            Employee designer = invocation.getArgument(1);
            property.setDesigner(designer);
            return property;
        });
        when(measures.findByPropertyOrderByMeasureTimeDesc(first)).thenReturn(List.of(scheduled, completed));
        when(measures.findByPropertyOrderByMeasureTimeDesc(second)).thenReturn(List.of());
        when(designs.findByCompany_ComIdAndCustomer_CustIdOrderByVersionDesc(1L, 20L))
                .thenReturn(List.of(active, archived));
        when(requirements.findByCompany_ComIdAndCustomer_CustIdOrderByRequirementIdDesc(1L, 20L))
                .thenReturn(List.of(requirement));
        when(customers.save(customer)).thenReturn(customer);

        Handoff handoff = new Handoff(customers, employees, dataScope, notices, properties,
                propertyRepository, measures, designs, requirements);
        ReflectionTestUtils.setField(handoff, "employee", boss);
        ReflectionTestUtils.setField(handoff, "customerId", 20L);
        ReflectionTestUtils.setField(handoff, "propertyId", 101L);
        ReflectionTestUtils.setField(handoff, "designerId", 11L);

        Customer saved = handoff.call(customer);

        assertThat(saved.getDesigner()).isSameAs(next);
        assertThat(first.getDesigner()).isSameAs(next);
        assertThat(second.getDesigner()).isSameAs(next);
        assertThat(scheduled.getDesigner()).isSameAs(next);
        assertThat(completed.getDesigner()).isSameAs(previous);
        assertThat(active.getOwner()).isSameAs(next);
        assertThat(archived.getOwner()).isSameAs(previous);
        assertThat(requirement.getOwner()).isSameAs(next);
        verify(notices).designerSubmitted(customer, boss, next);
    }

    /** DEC-065：新客户（待首联）允许店长当场派单，客户状态不随派单推进。 */
    @Test
    void storeManagerShouldAssignDesignerForNewCustomerBeforeFirstFollowUp() {
        Company company = company(1L);
        Store store = store(7L);
        Employee manager = employee(9L, "王宏文", EmployeeRole.StoreManager, company);
        manager.setStore(store);
        Employee designer = employee(11L, "董夏鹏", EmployeeRole.Designer, company);
        designer.setStore(store);

        Customer customer = new Customer();
        customer.setCustId(20L);
        customer.setName("陈女士");
        customer.setCompany(company);
        customer.setStore(store);
        customer.setStatus(CustStatus.New);

        Property property = property(101L, customer, null);
        property.setStore(store);

        CustomerRepository customers = mock(CustomerRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        DataScopeContext dataScope = mock(DataScopeContext.class);
        NoticeContext notices = mock(NoticeContext.class);
        PropertyContext properties = mock(PropertyContext.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        MeasureRepository measures = mock(MeasureRepository.class);
        DesignRepository designs = mock(DesignRepository.class);
        RequirementRepository requirements = mock(RequirementRepository.class);

        when(dataScope.requireEmployee(manager)).thenReturn(manager);
        when(dataScope.resolve(manager)).thenReturn(new DataScope(DataScopeType.Store, 1L, 7L, null, 9L));
        when(customers.findOne(any(Specification.class))).thenReturn(Optional.of(customer));
        when(employees.findByEmpIdAndCompany_ComId(11L, 1L)).thenReturn(designer);
        when(properties.resolve(101L, customer, manager)).thenReturn(property);
        when(propertyRepository.findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(1L, 20L))
                .thenReturn(List.of(property));
        when(properties.assignDesigner(any(), any())).thenAnswer(invocation -> {
            Property target = invocation.getArgument(0);
            Employee assigned = invocation.getArgument(1);
            target.setDesigner(assigned);
            return target;
        });
        when(measures.findByPropertyOrderByMeasureTimeDesc(property)).thenReturn(List.of());
        when(designs.findByCompany_ComIdAndCustomer_CustIdOrderByVersionDesc(1L, 20L)).thenReturn(List.of());
        when(requirements.findByCompany_ComIdAndCustomer_CustIdOrderByRequirementIdDesc(1L, 20L)).thenReturn(List.of());
        when(customers.save(customer)).thenReturn(customer);

        Handoff handoff = new Handoff(customers, employees, dataScope, notices, properties,
                propertyRepository, measures, designs, requirements);
        ReflectionTestUtils.setField(handoff, "employee", manager);
        ReflectionTestUtils.setField(handoff, "customerId", 20L);
        ReflectionTestUtils.setField(handoff, "propertyId", 101L);
        ReflectionTestUtils.setField(handoff, "designerId", 11L);

        Customer saved = handoff.call(customer);

        assertThat(saved.getDesigner()).isSameAs(designer);
        assertThat(saved.getStatus()).isEqualTo(CustStatus.New);
        assertThat(saved.getHandedOffAt()).isNotNull();
        assertThat(property.getDesigner()).isSameAs(designer);
        verify(notices).designerSubmitted(customer, manager, designer);
    }

    /** 已流失客户仍需先恢复再指派，提示语不得再与"待首联"混淆。 */
    @Test
    void lostCustomerShouldNotAcceptDesignerUntilRestored() {
        Company company = company(1L);
        Store store = store(7L);
        Employee manager = employee(9L, "王宏文", EmployeeRole.StoreManager, company);
        manager.setStore(store);

        Customer customer = new Customer();
        customer.setCustId(20L);
        customer.setName("陈女士");
        customer.setCompany(company);
        customer.setStore(store);
        customer.setStatus(CustStatus.Lost);

        CustomerRepository customers = mock(CustomerRepository.class);
        DataScopeContext dataScope = mock(DataScopeContext.class);
        when(dataScope.requireEmployee(manager)).thenReturn(manager);
        when(dataScope.resolve(manager)).thenReturn(new DataScope(DataScopeType.Store, 1L, 7L, null, 9L));
        when(customers.findOne(any(Specification.class))).thenReturn(Optional.of(customer));

        Handoff handoff = new Handoff(customers, mock(EmployeeRepository.class), dataScope,
                mock(NoticeContext.class), mock(PropertyContext.class), mock(PropertyRepository.class),
                mock(MeasureRepository.class), mock(DesignRepository.class), mock(RequirementRepository.class));
        ReflectionTestUtils.setField(handoff, "employee", manager);
        ReflectionTestUtils.setField(handoff, "customerId", 20L);
        ReflectionTestUtils.setField(handoff, "designerId", 11L);

        assertThatThrownBy(() -> handoff.call(customer))
                .hasMessageContaining("客户已流失");
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

    private Store store(Long id) {
        Store store = new Store();
        store.setStoreId(id);
        return store;
    }

    private Property property(Long id, Customer customer, Employee designer) {
        Property property = new Property();
        property.setPropertyId(id);
        property.setPropertyNo("P" + id);
        property.setCompany(customer.getCompany());
        property.setCustomer(customer);
        property.setDesigner(designer);
        property.setAddress("香樟花园" + id);
        return property;
    }

    private Measure measure(Long id, Property property, Employee designer, MeasureStatus status) {
        Measure measure = new Measure();
        measure.setMeasureId(id);
        measure.setProperty(property);
        measure.setDesigner(designer);
        measure.setStatus(status);
        return measure;
    }

    private Design design(Long id, Customer customer, Property property, Employee owner, DesignStatus status) {
        Design design = new Design();
        design.setDesignId(id);
        design.setNo("D" + id);
        design.setName("设计" + id);
        design.setCompany(customer.getCompany());
        design.setCustomer(customer);
        design.setProperty(property);
        design.setOwner(owner);
        design.setStatus(status);
        design.setArea(new java.math.BigDecimal("118"));
        return design;
    }

    private Requirement requirement(Long id, Customer customer, Property property, Employee owner) {
        Requirement requirement = new Requirement();
        requirement.setRequirementId(id);
        requirement.setCustomer(customer);
        requirement.setProperty(property);
        requirement.setOwner(owner);
        requirement.setTitle("客户诉求");
        return requirement;
    }
}
