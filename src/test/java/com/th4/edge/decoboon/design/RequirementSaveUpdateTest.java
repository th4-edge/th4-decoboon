package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.RequirementCategory;
import com.th4.edge.decoboon.design.configure.RequirementKind;
import com.th4.edge.decoboon.design.configure.RequirementPriority;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.design.service.requirements.save.support.Save;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequirementSaveUpdateTest {

    @Test
    void shouldUpdateExistingRequirementInsteadOfCreatingDuplicate() {
        Employee designer = employee(10L);
        Customer customer = customer(20L, designer);
        Property property = property(101L, customer, designer);
        Requirement existing = requirement(301L, customer, property);
        Requirement request = new Requirement();
        request.setKind(RequirementKind.Demand);
        request.setCategory(RequirementCategory.Other);
        request.setPriority(RequirementPriority.High);
        request.setTitle("量房关注点");
        request.setDetail("房屋信息：层高2.8m\n客户想法：需要更多收纳");
        request.setResponse("采用定制柜");

        DesignContext designContext = mock(DesignContext.class);
        RequirementRepository repository = mock(RequirementRepository.class);
        PropertyContext propertyContext = mock(PropertyContext.class);
        when(designContext.requireEmployee(designer)).thenReturn(designer);
        when(designContext.requireCustomer(20L, designer)).thenReturn(customer);
        when(designContext.resolveOwner(designer, null, designer)).thenReturn(designer);
        when(propertyContext.resolve(101L, customer, designer)).thenReturn(property);
        when(repository.findById(301L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Requirement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Save save = new Save(designContext, repository, propertyContext);
        ReflectionTestUtils.setField(save, "customerId", 20L);
        ReflectionTestUtils.setField(save, "propertyId", 101L);
        ReflectionTestUtils.setField(save, "requirementId", 301L);
        ReflectionTestUtils.setField(save, "employee", designer);

        Requirement saved = save.call(request);

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getRequirementId()).isEqualTo(301L);
        assertThat(saved.getTitle()).isEqualTo("量房关注点");
        assertThat(saved.getDetail()).contains("更多收纳");
        assertThat(saved.getResponse()).isEqualTo("采用定制柜");
    }

    private Employee employee(Long id) {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(EmployeeRole.Designer);
        employee.setCompany(company);
        return employee;
    }

    private Customer customer(Long id, Employee designer) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setCompany(designer.getCompany());
        customer.setDesigner(designer);
        customer.setStatus(CustStatus.Quoted);
        return customer;
    }

    private Property property(Long id, Customer customer, Employee designer) {
        Property property = new Property();
        property.setPropertyId(id);
        property.setCompany(customer.getCompany());
        property.setCustomer(customer);
        property.setDesigner(designer);
        return property;
    }

    private Requirement requirement(Long id, Customer customer, Property property) {
        Requirement requirement = new Requirement();
        requirement.setRequirementId(id);
        requirement.setCompany(customer.getCompany());
        requirement.setCustomer(customer);
        requirement.setProperty(property);
        return requirement;
    }
}
