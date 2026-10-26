package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DesignWritePermissionTest {

    private final DataScopeContext dataScopeContext = mock(DataScopeContext.class);
    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final DesignContext context = new DesignContext(
            dataScopeContext, null, null, null, null, null, null);
    private final DesignContext editableContext = new DesignContext(
            dataScopeContext, null, null, null, quoteRepository, projectRepository, null);

    @Test
    void shouldRejectRolesWithoutDesignWriteCapability() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Marketer, EmployeeRole.Pm, EmployeeRole.Finance}) {
            Employee employee = employee(role);
            when(dataScopeContext.requireEmployee(employee)).thenReturn(employee);

            assertThatThrownBy(() -> context.requireDesignWriteAccess(employee))
                    .isInstanceOf(DefinedException.class)
                    .hasMessageContaining("无权新增或复制设计方案");
        }
    }

    @Test
    void shouldAllowDesignWriteRoles() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Boss, EmployeeRole.StoreManager,
                EmployeeRole.TeamLeader, EmployeeRole.Designer}) {
            Employee employee = employee(role);
            when(dataScopeContext.requireEmployee(employee)).thenReturn(employee);

            assertThat(context.requireDesignWriteAccess(employee)).isSameAs(employee);
        }
    }

    @Test
    void readyDesignWithOpenPropertyShouldBeEditable() {
        Employee designer = employee(EmployeeRole.Designer, 10L);
        Design design = design(20L, DesignStatus.Ready, 30L);
        when(quoteRepository.existsByProperty_PropertyIdAndStatusIn(eq(30L), anyList())).thenReturn(false);
        when(projectRepository.existsByCompany_ComIdAndProperty_PropertyId(1L, 30L)).thenReturn(false);

        assertThat(editableContext.canEdit(design, designer)).isTrue();
    }

    @Test
    void confirmedQuoteOrProjectMustLockReadyDesign() {
        Employee designer = employee(EmployeeRole.Designer, 10L);
        Design design = design(20L, DesignStatus.Ready, 30L);
        when(projectRepository.existsByCompany_ComIdAndProperty_PropertyId(1L, 30L)).thenReturn(false);

        when(quoteRepository.existsByProperty_PropertyIdAndStatusIn(eq(30L), anyList())).thenReturn(true);
        assertThat(editableContext.canEdit(design, designer)).isFalse();

        when(quoteRepository.existsByProperty_PropertyIdAndStatusIn(eq(30L), anyList())).thenReturn(false);
        when(projectRepository.existsByCompany_ComIdAndProperty_PropertyId(1L, 30L)).thenReturn(true);
        assertThat(editableContext.canEdit(design, designer)).isFalse();
    }

    @Test
    void readOnlyRoleArchivedDesignMustNotBeEditable() {
        Employee finance = employee(EmployeeRole.Finance, 11L);
        Employee designer = employee(EmployeeRole.Designer, 12L);
        Design archived = design(21L, DesignStatus.Archived, 31L);

        assertThat(editableContext.canEdit(archived, finance)).isFalse();
        assertThat(editableContext.canEdit(archived, designer)).isFalse();
        assertThatThrownBy(() -> editableContext.requireEditable(archived))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("方案已归档");
        verifyNoInteractions(quoteRepository, projectRepository);
    }

    private Employee employee(EmployeeRole role) {
        return employee(role, (long) role.ordinal() + 1);
    }

    private Employee employee(EmployeeRole role, Long empId) {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setName(role.name());
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }

    private Design design(Long designId, DesignStatus status, Long propertyId) {
        Company company = new Company();
        company.setComId(1L);
        Property property = new Property();
        property.setPropertyId(propertyId);
        property.setCompany(company);
        Design design = new Design();
        design.setDesignId(designId);
        design.setStatus(status);
        design.setProperty(property);
        return design;
    }
}
