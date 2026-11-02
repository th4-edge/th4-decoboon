package com.th4.edge.decoboon.pack;

import com.th4.edge.decoboon.pack.configure.PackageTemplateStatus;
import com.th4.edge.decoboon.pack.context.PackageCostContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 套餐管理列表必须能看到草稿，报价选择器必须继续只读取当前发布版本。 */
class PackageTemplatePageScopeTest {

    @Test
    void managementPageDoesNotApplyPublishedPickerFilter() {
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<Object> statusPath = mock(Path.class);
        Path<Boolean> currentPath = mock(Path.class);

        evaluateCapturedSpecification(false, builder, statusPath, currentPath);

        verify(builder, never()).equal(statusPath, PackageTemplateStatus.Published);
        verify(builder, never()).isTrue(currentPath);
    }

    @Test
    void quotePickerPageOnlyAppliesPublishedCurrentFilter() {
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<Object> statusPath = mock(Path.class);
        Path<Boolean> currentPath = mock(Path.class);
        Predicate published = mock(Predicate.class);
        Predicate current = mock(Predicate.class);
        when(builder.equal(statusPath, PackageTemplateStatus.Published)).thenReturn(published);
        when(builder.isTrue(currentPath)).thenReturn(current);

        evaluateCapturedSpecification(true, builder, statusPath, currentPath);

        verify(builder).equal(statusPath, PackageTemplateStatus.Published);
        verify(builder).isTrue(currentPath);
    }

    private void evaluateCapturedSpecification(boolean picker, CriteriaBuilder builder, Path<Object> statusPath) {
        evaluateCapturedSpecification(picker, builder, statusPath, mock(Path.class));
    }

    private void evaluateCapturedSpecification(boolean picker, CriteriaBuilder builder, Path<Object> statusPath,
                                              Path<Boolean> currentPath) {
        Company company = new Company();
        company.setComId(100L);
        Employee employee = new Employee();
        employee.setEmpId(1L);
        employee.setCompany(company);

        PackageTemplateRepository templates = mock(PackageTemplateRepository.class);
        PackageLineRepository lines = mock(PackageLineRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        PackageCostContext costs = mock(PackageCostContext.class);
        when(tenant.requireEmployee(employee)).thenReturn(employee);
        Page<PackageTemplate> empty = new PageImpl<>(List.of());
        when(templates.findAll(any(Specification.class), any(Pageable.class))).thenReturn(empty);
        when(costs.mask(any(Page.class), eq(employee))).thenAnswer(invocation -> invocation.getArgument(0));

        var page = new com.th4.edge.decoboon.pack.service.templates.page.support.Page(
                templates, lines, tenant, costs);
        ReflectionTestUtils.setField(page, "employee", employee);
        ReflectionTestUtils.setField(page, "publishedOnly", picker);
        page.call(new PackageTemplate());

        ArgumentCaptor<Specification<PackageTemplate>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(templates).findAll(captor.capture(), any(Pageable.class));

        Root<PackageTemplate> root = mock(Root.class);
        Path<Object> companyPath = mock(Path.class);
        Path<Object> companyIdPath = mock(Path.class);
        when(root.get("company")).thenReturn(companyPath);
        when(companyPath.get("comId")).thenReturn(companyIdPath);
        when(root.get("status")).thenReturn(statusPath);
        when(root.<Boolean>get("currentPublished")).thenReturn(currentPath);

        Predicate predicate = captor.getValue().toPredicate(root, mock(CriteriaQuery.class), builder);
        assertThat(predicate).isNull();
        verify(builder).equal(companyIdPath, 100L);
    }
}
