package com.th4.edge.decoboon.property;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.property.service.properties.page.support.Page;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
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

/** 项目经理通过项目参与关系只读查看本项目房屋。 */
class PropertyProjectManagerVisibilityTest {

    @Test
    void projectManagerPropertyPageComesFromManagedProjects() {
        PropertyRepository properties = mock(PropertyRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        Employee manager = employee(10L, EmployeeRole.Pm);
        Property property = property(100L);
        Project project = new Project();
        project.setProperty(property);

        when(tenant.requireEmployee(manager)).thenReturn(manager);
        when(projects.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(1L, 10L))
                .thenReturn(List.of(project));

        Page page = new Page(properties, projects, tenant, mock(PropertyContext.class));
        ReflectionTestUtils.setField(page, "employee", manager);

        assertThat(page.call(new Property()).getContent())
                .extracting(Property::getPropertyId)
                .containsExactly(100L);
    }

    @Test
    void projectManagerCanReadOnlyPropertiesLinkedToManagedProjects() {
        PropertyRepository properties = mock(PropertyRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        Employee manager = employee(10L, EmployeeRole.Pm);
        Employee otherManager = employee(11L, EmployeeRole.Pm);
        Property property = property(100L);
        property.setCompany(manager.getCompany());

        when(projects.existsByCompany_ComIdAndProperty_PropertyIdAndManager_EmpId(1L, 100L, 10L))
                .thenReturn(true);
        when(projects.existsByCompany_ComIdAndProperty_PropertyIdAndManager_EmpId(1L, 100L, 11L))
                .thenReturn(false);

        PropertyContext context = new PropertyContext(properties, projects, mock(DataScopeContext.class));

        assertThat(context.canView(property, manager)).isTrue();
        assertThat(context.canView(property, otherManager)).isFalse();
    }

    private static Employee employee(Long empId, EmployeeRole role) {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setCompany(company);
        employee.setRole(role);
        return employee;
    }

    private static Property property(Long propertyId) {
        Property property = new Property();
        property.setPropertyId(propertyId);
        return property;
    }
}
