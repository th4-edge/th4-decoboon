package com.th4.edge.decoboon.catalog;

import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.catalog.configure.CatalogRevisionType;
import com.th4.edge.decoboon.catalog.model.CatalogItemRevision;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRevisionRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogRevisionPageTest {

    @Test
    void nonCostRoleReceivesRevisionMetadataWithoutCostSnapshot() {
        CatalogItemRevision revision = revision();
        Employee designer = employee(EmployeeRole.Designer);
        revision.setChangedBy(designer);
        CatalogItemRevisionRepository repository = mock(CatalogItemRevisionRepository.class);
        CatalogContext context = mock(CatalogContext.class);
        when(context.requireEmployee(designer)).thenReturn(designer);
        when(repository.findByCompany_ComIdAndItemId(eq(1L), eq(20L), any()))
                .thenReturn(new PageImpl<>(List.of(revision)));

        com.th4.edge.decoboon.catalog.service.revisions.page.support.Page provider =
                new com.th4.edge.decoboon.catalog.service.revisions.page.support.Page(
                        repository, context, new CatalogCostContext());
        ReflectionTestUtils.setField(provider, "itemId", 20L);
        ReflectionTestUtils.setField(provider, "employee", designer);

        CatalogItemRevision result = provider.call(new CatalogItemRevision()).getContent().get(0);

        assertThat(result.getRevision()).isEqualTo(4);
        assertThat(result.getChangedBy()).isSameAs(designer);
        assertThat(result.getSnapshotJson()).isNull();
    }

    @Test
    void costRoleReceivesImmutableSnapshotValues() {
        CatalogItemRevision revision = revision();
        Employee finance = employee(EmployeeRole.Finance);
        CatalogItemRevisionRepository repository = mock(CatalogItemRevisionRepository.class);
        CatalogContext context = mock(CatalogContext.class);
        when(context.requireEmployee(finance)).thenReturn(finance);
        when(repository.findByCompany_ComIdAndItemId(eq(1L), eq(20L), any()))
                .thenReturn(new PageImpl<>(List.of(revision)));

        com.th4.edge.decoboon.catalog.service.revisions.page.support.Page provider =
                new com.th4.edge.decoboon.catalog.service.revisions.page.support.Page(
                        repository, context, new CatalogCostContext());
        ReflectionTestUtils.setField(provider, "itemId", 20L);
        ReflectionTestUtils.setField(provider, "employee", finance);

        CatalogItemRevision result = provider.call(new CatalogItemRevision()).getContent().get(0);

        assertThat(result.getSnapshotJson()).contains("\"costPrice\":70").contains("\"revision\":4");
        assertThat(result.getSnapshotHash()).hasSize(64);
    }

    private CatalogItemRevision revision() {
        CatalogItemRevision revision = new CatalogItemRevision();
        revision.setRevision(4);
        revision.setChangeType(CatalogRevisionType.Update);
        revision.setSnapshotJson("{\"name\":\"墙砖\",\"costPrice\":70,\"revision\":4}");
        revision.setSnapshotHash("a".repeat(64));
        revision.setChangedBy(employee(EmployeeRole.Finance));
        return revision;
    }

    private Employee employee(EmployeeRole role) {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(9L);
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }
}
