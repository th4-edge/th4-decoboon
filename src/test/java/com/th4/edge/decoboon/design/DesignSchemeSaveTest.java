package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignCopyContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.service.CadConvertService;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignSchemeSaveTest {

    @Test
    void shouldCreateDraftWithZeroAreaWhenCustomerAndPropertyAreaAreMissing() {
        Employee designer = employee(10L);
        Customer customer = customer(20L, designer);
        Property property = property(101L, customer, designer);
        Design request = new Design();
        request.setName("现代简约方案");

        DesignContext designContext = mock(DesignContext.class);
        DesignRepository designRepository = mock(DesignRepository.class);
        PropertyContext propertyContext = mock(PropertyContext.class);
        when(designContext.requireDesignWriteAccess(designer)).thenReturn(designer);
        when(designContext.requireCustomer(20L, designer)).thenReturn(customer);
        when(designContext.resolveOwner(designer, null, designer)).thenReturn(designer);
        when(propertyContext.resolve(101L, customer, designer)).thenReturn(property);
        when(designRepository.countByCompany_ComIdAndCustomer_CustId(1L, 20L)).thenReturn(0L);
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.th4.edge.decoboon.design.service.schemes.save.support.Save save =
                new com.th4.edge.decoboon.design.service.schemes.save.support.Save(
                        designContext, mock(DesignCopyContext.class), designRepository,
                        propertyContext, mock(CadConvertService.class));
        ReflectionTestUtils.setField(save, "customerId", 20L);
        ReflectionTestUtils.setField(save, "propertyId", 101L);
        ReflectionTestUtils.setField(save, "employee", designer);

        Design saved = save.call(request);

        assertThat(saved.getArea()).isNotNull().isEqualByComparingTo("0");
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
        customer.setName("测试客户");
        return customer;
    }

    private Property property(Long id, Customer customer, Employee designer) {
        Property property = new Property();
        property.setPropertyId(id);
        property.setCompany(customer.getCompany());
        property.setCustomer(customer);
        property.setDesigner(designer);
        property.setArea(null);
        return property;
    }
}
