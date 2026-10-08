package com.th4.edge.decoboon.pack;

import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.pack.context.PackageConfigContext;
import com.th4.edge.decoboon.pack.context.PackageContext;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageSectionRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateItemRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PackageTemplateMaintainRoleTest {

    @Test
    void storeManagerCanMaintainPackageTemplate() {
        TenantContext tenant = mock(TenantContext.class);
        PackageSectionRepository sections = mock(PackageSectionRepository.class);
        PackageConfigContext context = context(tenant, sections);
        Employee storeManager = employee(EmployeeRole.StoreManager);
        PackageSection source = new PackageSection();
        source.setName("基础施工");

        when(tenant.requireRole(eq(storeManager), eq(EmployeeRole.Boss), eq(EmployeeRole.StoreManager)))
                .thenReturn(storeManager);
        when(sections.save(any(PackageSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PackageSection saved = context.saveSection(source, storeManager);

        assertThat(saved.getName()).isEqualTo("基础施工");
        assertThat(saved.getCompany()).isSameAs(storeManager.getCompany());
    }

    @Test
    void designerCannotMaintainPackageTemplate() {
        TenantContext tenant = mock(TenantContext.class);
        PackageConfigContext context = context(tenant, mock(PackageSectionRepository.class));
        Employee designer = employee(EmployeeRole.Designer);
        when(tenant.requireRole(eq(designer), eq(EmployeeRole.Boss), eq(EmployeeRole.StoreManager)))
                .thenThrow(new DefinedException(new DefaultErrorCode("403", "当前账号无权执行该操作")));

        assertThatThrownBy(() -> context.saveSection(new PackageSection(), designer))
                .hasMessageContaining("无权执行该操作");
    }

    private PackageConfigContext context(TenantContext tenant, PackageSectionRepository sections) {
        return new PackageConfigContext(
                mock(PackageTemplateRepository.class),
                sections,
                mock(PackageLineRepository.class),
                mock(PackageTemplateItemRepository.class),
                mock(CatalogItemRepository.class),
                mock(QuoteLineRepository.class),
                mock(PackageContext.class),
                tenant);
    }

    private Employee employee(EmployeeRole role) {
        Company company = new Company();
        company.setComId(100L);
        Employee employee = new Employee();
        employee.setEmpId((long) role.ordinal() + 1);
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }
}
