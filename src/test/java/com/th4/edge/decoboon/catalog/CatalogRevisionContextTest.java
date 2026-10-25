package com.th4.edge.decoboon.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.CatalogRevisionType;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.context.CatalogRevisionContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.model.CatalogItemRevision;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRevisionRepository;
import com.th4.edge.decoboon.catalog.support.CatalogQuoteSourceSupport;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogRevisionContextTest {

    private final CatalogItemRevisionRepository repository = mock(CatalogItemRevisionRepository.class);
    private final CatalogRevisionContext context = new CatalogRevisionContext(repository, new ObjectMapper());

    @Test
    void revisionIncrementsAndAuditSnapshotIsImmutable() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(2L);
        CatalogItem item = item(company);
        item.setRevision(4);
        when(repository.save(any(CatalogItemRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int revision = context.nextRevision(item);
        CatalogItemRevision audit = context.record(item, employee, CatalogRevisionType.Update);

        assertThat(revision).isEqualTo(5);
        assertThat(audit.getRevision()).isEqualTo(5);
        assertThat(audit.getSnapshotJson()).contains("墙砖").contains("\"revision\":5");
        assertThat(audit.getSnapshotHash()).hasSize(64);
        assertThat(audit.getChangedBy()).isSameAs(employee);
    }

    @Test
    void quoteLineCarriesCatalogSourceRevisionAndValues() {
        Company company = new Company();
        company.setComId(1L);
        CatalogItem item = item(company);
        item.setRevision(7);
        QuoteLine line = new QuoteLine();

        CatalogQuoteSourceSupport.attach(line, item);

        assertThat(line.getCatalogItem()).isSameAs(item);
        assertThat(line.getCatalogSourceId()).isEqualTo(20L);
        assertThat(line.getCatalogSourceCode()).isEqualTo("ZC-1");
        assertThat(line.getCatalogSourceRevision()).isEqualTo(7);

        item.setRevision(8);
        QuoteLine copied = new QuoteLine();
        CatalogQuoteSourceSupport.copy(line, copied);

        assertThat(line.getCatalogSourceRevision()).isEqualTo(7);
        assertThat(copied.getCatalogSourceRevision()).isEqualTo(7);
    }

    @Test
    void effectiveChangeIgnoresRevisionSortAndMoneyScaleButDetectsBusinessValues() {
        Company company = new Company();
        company.setComId(1L);
        CatalogItem before = item(company);
        before.setRevision(3);
        before.setSort(0);
        before.setSourceType(com.th4.edge.decoboon.catalog.configure.CatalogSourceType.Manual);

        CatalogItem after = item(company);
        after.setRevision(3);
        after.setSort(99);
        after.setSourceType(com.th4.edge.decoboon.catalog.configure.CatalogSourceType.Import);
        after.setMaterialPrice(new BigDecimal("100.00"));

        assertThat(context.effectiveChange(before, after)).isFalse();

        after.setCostPrice(new BigDecimal("71.00"));

        assertThat(context.effectiveChange(before, after)).isTrue();
    }

    private CatalogItem item(Company company) {
        CatalogItem item = new CatalogItem();
        item.setItemId(20L);
        item.setCompany(company);
        item.setType(ItemType.Material);
        item.setCode("ZC-1");
        item.setCategory("瓷砖");
        item.setName("墙砖");
        item.setUnit(Unit.Area);
        item.setPricingMode(PricingMethod.MaterialOnly);
        item.setMaterialPrice(new BigDecimal("100"));
        item.setManualPrice(BigDecimal.ZERO);
        item.setCostPrice(new BigDecimal("70"));
        item.setFormula("材料单价 × 工程量");
        item.setImages(List.of());
        item.setActive(Boolean.TRUE);
        item.setSort(0);
        return item;
    }
}
