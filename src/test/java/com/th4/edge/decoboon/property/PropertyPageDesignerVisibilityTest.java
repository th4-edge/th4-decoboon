package com.th4.edge.decoboon.property;

import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.property.service.properties.page.support.Page;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 设计师作为业务负责人时，房屋及相关量房待办必须可见。 */
class PropertyPageDesignerVisibilityTest {

    @Test
    void designerSeesOwnedSoldAndDesignedPropertiesWithoutDuplicates() {
        PropertyRepository properties = mock(PropertyRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        Employee designer = employee(10L);
        Property owned = property(100L);
        Property shared = property(200L);

        when(tenant.requireEmployee(designer)).thenReturn(designer);
        when(properties.findByCompany_ComIdAndDesigner_EmpIdOrderByPropertyIdDesc(1L, 10L))
                .thenReturn(List.of(shared));
        when(properties.findByCompany_ComIdAndOwner_EmpIdOrderByPropertyIdDesc(1L, 10L))
                .thenReturn(List.of(owned, shared));
        when(properties.findByCompany_ComIdAndSalesman_EmpIdOrderByPropertyIdDesc(1L, 10L))
                .thenReturn(List.of(owned));

        Page page = new Page(properties, tenant, mock(PropertyContext.class));
        ReflectionTestUtils.setField(page, "employee", designer);

        assertThat(page.call(new Property()).getContent())
                .extracting(Property::getPropertyId)
                .containsExactly(shared.getPropertyId(), owned.getPropertyId());
    }

    private static Employee employee(Long empId) {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setCompany(company);
        employee.setRole(EmployeeRole.Designer);
        return employee;
    }

    private static Property property(Long propertyId) {
        Property property = new Property();
        property.setPropertyId(propertyId);
        return property;
    }
}
