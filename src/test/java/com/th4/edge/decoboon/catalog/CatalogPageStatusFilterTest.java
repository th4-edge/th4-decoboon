package com.th4.edge.decoboon.catalog;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 报价项目库管理页可按状态筛选；默认列表仍只返回启用项。 */
class CatalogPageStatusFilterTest {

    @Test
    void inactiveStatusAppliesExactFalsePredicateEvenWhenIncludingInactive() {
        Fixture fixture = fixture(true, false);

        fixture.evaluate();

        verify(fixture.builder).equal(fixture.activePath, false);
        verify(fixture.builder, never()).isTrue(fixture.activePath);
    }

    @Test
    void activeStatusAppliesExactTruePredicate() {
        Fixture fixture = fixture(true, true);

        fixture.evaluate();

        verify(fixture.builder).equal(fixture.activePath, true);
        verify(fixture.builder, never()).isTrue(fixture.activePath);
    }

    @Test
    void allStatusIncludesBothStates() {
        Fixture fixture = fixture(true, null);

        fixture.evaluate();

        verify(fixture.builder, never()).equal(eq(fixture.activePath), any());
        verify(fixture.builder, never()).isTrue(fixture.activePath);
    }

    @Test
    void defaultStatusStillOnlyReturnsActiveItems() {
        Fixture fixture = fixture(false, null);

        fixture.evaluate();

        verify(fixture.builder).isTrue(fixture.activePath);
        verify(fixture.builder, never()).equal(eq(fixture.activePath), any());
    }

    private Fixture fixture(Boolean includeInactive, Boolean active) {
        Company company = new Company();
        company.setComId(100L);
        Employee employee = new Employee();
        employee.setEmpId(1L);
        employee.setCompany(company);

        CatalogItemRepository repository = mock(CatalogItemRepository.class);
        CatalogContext context = mock(CatalogContext.class);
        when(context.requireEmployee(employee)).thenReturn(employee);
        Page<CatalogItem> empty = new PageImpl<>(List.of());
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(empty);

        var page = new com.th4.edge.decoboon.catalog.service.items.page.support.Page(
                repository, new CatalogCostContext(), context);
        ReflectionTestUtils.setField(page, "type", ItemType.Material);
        ReflectionTestUtils.setField(page, "employee", employee);
        ReflectionTestUtils.setField(page, "includeInactive", includeInactive);
        ReflectionTestUtils.setField(page, "active", active);
        page.call(new CatalogItem());

        ArgumentCaptor<Specification<CatalogItem>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(captor.capture(), any(Pageable.class));
        return new Fixture(captor.getValue(), employee);
    }

    private static final class Fixture {
        private final Specification<CatalogItem> specification;
        private final Employee employee;
        private final CriteriaBuilder builder = mock(CriteriaBuilder.class);
        private final Path<Boolean> activePath = mock(Path.class);

        private Fixture(Specification<CatalogItem> specification, Employee employee) {
            this.specification = specification;
            this.employee = employee;
        }

        private void evaluate() {
            Root<CatalogItem> root = mock(Root.class);
            Path<Object> companyPath = mock(Path.class);
            Path<Object> companyIdPath = mock(Path.class);
            Path<Object> typePath = mock(Path.class);
            when(root.get("company")).thenReturn(companyPath);
            when(companyPath.get("comId")).thenReturn(companyIdPath);
            when(root.get("type")).thenReturn(typePath);
            when(root.<Boolean>get("active")).thenReturn(activePath);

            specification.toPredicate(root, mock(CriteriaQuery.class), builder);

            verify(builder).equal(companyIdPath, employee.getCompany().getComId());
            verify(builder).equal(typePath, ItemType.Material);
        }
    }
}
