package com.th4.edge.decoboon.catalog;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.model.CatalogTree;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.catalog.service.categories.tree.support.Tree;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogTreeProjectionTest {

    @Test
    void shouldExposeSeriesFacetsAndNotTurnMissingValuesIntoOther() {
        CatalogItemRepository repository = mock(CatalogItemRepository.class);
        CatalogContext context = mock(CatalogContext.class);
        Employee employee = employee();
        when(context.requireEmployee(employee)).thenReturn(employee);
        when(repository.findFacetsByCompanyAndTypeAndActive(1L, ItemType.Material, true)).thenReturn(List.of(
                new Object[]{"瓷砖", null, null, null},
                new Object[]{"瓷砖", "马可波罗", "800系列", "800*800"}
        ));
        when(repository.countByCompany_ComIdAndTypeAndActive(1L, ItemType.Material, true)).thenReturn(2L);

        Tree provider = new Tree(repository, context);
        ReflectionTestUtils.setField(provider, "type", ItemType.Material);
        ReflectionTestUtils.setField(provider, "employee", employee);

        CatalogTree tree = provider.call(new CatalogItem());

        assertThat(tree.getBrands().get("瓷砖")).containsExactly("马可波罗").doesNotContain("");
        assertThat(tree.getSpecs().get("瓷砖")).containsExactly("800*800");
        assertThat(tree.getSeriesByBrand().get("瓷砖").get("马可波罗")).containsExactly("800系列");
        assertThat(tree.getSpecsBySeries().get("瓷砖").get("马可波罗").get("800系列")).containsExactly("800*800");

        AllowProperties allow = Tree.class.getAnnotation(Properties.class).allow()[0];
        assertThat(allow.properties()).contains("seriesByBrand", "specsBySeries");
    }

    @Test
    void shouldProjectInactiveFacetsWhenRequested() {
        CatalogItemRepository repository = mock(CatalogItemRepository.class);
        CatalogContext context = mock(CatalogContext.class);
        Employee employee = employee();
        when(context.requireEmployee(employee)).thenReturn(employee);
        when(repository.findFacetsByCompanyAndTypeAndActive(1L, ItemType.Material, false))
                .thenReturn(List.<Object[]>of(new Object[]{"旧瓷砖", "旧品牌", null, "600*600"}));
        when(repository.countByCompany_ComIdAndTypeAndActive(1L, ItemType.Material, false)).thenReturn(1L);

        Tree provider = new Tree(repository, context);
        ReflectionTestUtils.setField(provider, "type", ItemType.Material);
        ReflectionTestUtils.setField(provider, "includeInactive", true);
        ReflectionTestUtils.setField(provider, "active", false);
        ReflectionTestUtils.setField(provider, "employee", employee);

        CatalogTree tree = provider.call(new CatalogItem());

        assertThat(tree.getCategories()).containsExactly("旧瓷砖");
        assertThat(tree.getBrands().get("旧瓷砖")).containsExactly("旧品牌");
        assertThat(tree.getTotal()).isEqualTo(1L);
    }

    private Employee employee() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setCompany(company);
        return employee;
    }
}
