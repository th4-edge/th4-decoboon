package com.th4.edge.decoboon.catalog;

import com.th4.edge.decoboon.catalog.context.CatalogCodeContext;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.catalog.context.CatalogDetailContext;
import com.th4.edge.decoboon.catalog.context.CatalogRevisionContext;
import com.th4.edge.decoboon.catalog.context.PinyinContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.catalog.service.items.importer.ImportResult;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** JSON 导入必须保留成本空白的 null/明确 0 区分。 */
class CatalogImportCostBoundaryTest {

    @Test
    void nullCostMustRemainPendingInsteadOfBecomingZero() {
        assertImportedCost(null, null);
    }

    @Test
    void explicitZeroCostMustRemainZero() {
        assertImportedCost(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private void assertImportedCost(BigDecimal sourceCost, BigDecimal expectedCost) {
        CatalogItemRepository repository = mock(CatalogItemRepository.class);
        CatalogCostContext costContext = mock(CatalogCostContext.class);
        CatalogCodeContext codeContext = mock(CatalogCodeContext.class);
        CatalogContext context = mock(CatalogContext.class);
        CatalogDetailContext detailContext = mock(CatalogDetailContext.class);

        com.th4.edge.decoboon.catalog.service.items.importer.support.Import importer =
                new com.th4.edge.decoboon.catalog.service.items.importer.support.Import(
                        repository, costContext, new PinyinContext(), codeContext, context, detailContext,
                        mock(CatalogRevisionContext.class));

        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(10L);
        employee.setRole(EmployeeRole.Finance);
        employee.setCompany(company);

        CatalogItem source = new CatalogItem();
        source.setName("测试项目");
        source.setCostPrice(sourceCost);

        ReflectionTestUtils.setField(importer, "employee", employee);
        ReflectionTestUtils.setField(importer, "items", List.of(source));

        when(context.requireMaintainer(employee)).thenReturn(employee);
        when(context.formula(any())).thenReturn("材料单价 × 工程量");
        when(repository.save(any(CatalogItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResult result = importer.call(List.of());
        ArgumentCaptor<CatalogItem> saved = ArgumentCaptor.forClass(CatalogItem.class);

        assertThat(result.getSuccess()).isEqualTo(1);
        verifySavedItem(repository, saved, expectedCost);
    }

    private void verifySavedItem(CatalogItemRepository repository, ArgumentCaptor<CatalogItem> saved,
                                 BigDecimal expectedCost) {
        org.mockito.Mockito.verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCostPrice()).isEqualTo(expectedCost);
    }
}
