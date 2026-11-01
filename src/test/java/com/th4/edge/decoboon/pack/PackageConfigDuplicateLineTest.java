package com.th4.edge.decoboon.pack;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.pack.configure.PackageTemplateStatus;
import com.th4.edge.decoboon.pack.context.PackageConfigContext;
import com.th4.edge.decoboon.pack.context.PackageContext;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageSectionRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateItemRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PackageConfigDuplicateLineTest {

    @Test
    void rejectsSameCatalogItemInSameTemplateAndSection() {
        PackageTemplateRepository templates = mock(PackageTemplateRepository.class);
        PackageSectionRepository sections = mock(PackageSectionRepository.class);
        PackageLineRepository lines = mock(PackageLineRepository.class);
        CatalogItemRepository catalog = mock(CatalogItemRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        Employee employee = mock(Employee.class);
        Company company = mock(Company.class);
        PackageTemplate template = new PackageTemplate();
        template.setTemplateId(10L);
        template.setStatus(PackageTemplateStatus.Draft);
        PackageSection section = new PackageSection();
        section.setGroupId(20L);
        CatalogItem item = new CatalogItem();
        item.setItemId(30L);
        item.setActive(true);
        PackageLine existing = new PackageLine();
        existing.setItemId(40L);

        when(employee.getCompany()).thenReturn(company);
        when(company.getComId()).thenReturn(1L);
        when(tenant.requireRole(eq(employee), eq(EmployeeRole.Boss), eq(EmployeeRole.StoreManager)))
                .thenReturn(employee);
        when(templates.findByTemplateIdAndCompany_ComId(10L, 1L)).thenReturn(Optional.of(template));
        when(sections.findByGroupIdAndCompany_ComId(20L, 1L)).thenReturn(Optional.of(section));
        when(catalog.findByItemIdAndCompany_ComId(30L, 1L)).thenReturn(Optional.of(item));
        when(lines.findByTemplate_TemplateIdAndSection_GroupIdAndItem_ItemId(10L, 20L, 30L))
                .thenReturn(Optional.of(existing));

        PackageConfigContext context = new PackageConfigContext(
                templates, sections, lines, mock(PackageTemplateItemRepository.class), catalog,
                mock(QuoteLineRepository.class), mock(PackageContext.class), tenant);

        assertThatThrownBy(() -> context.saveLine(new PackageLine(), 10L, 20L, 30L, employee))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("已存在");
    }
}
