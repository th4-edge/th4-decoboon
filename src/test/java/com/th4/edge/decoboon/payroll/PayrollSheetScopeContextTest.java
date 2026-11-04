package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.context.PayrollSheetScopeContext;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayrollSheetScopeContextTest {

    @Test
    void shouldReturnOnlyCurrentStoreTotalsAndMarkMixedSheet() {
        Company company = company();
        Store hq = store(11L, "总店");
        Store store = store(12L, "滨湖店");
        PayrollSheet sheet = sheet(10L, company, hq, "2029-04");
        PayrollItem own = item(20L, company, store, sheet, "4400.00", "100.00", "4300.00");
        PayrollItem foreign = item(21L, company, hq, sheet, "5000.00", "0.00", "5000.00");
        PayrollItemRepository items = mock(PayrollItemRepository.class);
        when(items.findByCompany_ComIdAndSheet_SheetIdInOrderByEmployee_NameAsc(100L, List.of(10L)))
                .thenReturn(List.of(own, foreign));

        PayrollSheetScopeContext context = new PayrollSheetScopeContext(items);
        Page<PayrollSheet> result = context.scopePage(100L,
                new PageImpl<>(List.of(sheet), PageRequest.of(0, 20), 1), 12L);

        PayrollSheet scoped = result.getContent().get(0);
        assertThat(scoped.getTotalGrossAmount()).isEqualByComparingTo("4400.00");
        assertThat(scoped.getTotalDeductionAmount()).isEqualByComparingTo("100.00");
        assertThat(scoped.getTotalNetAmount()).isEqualByComparingTo("4300.00");
        assertThat(scoped.getMixedStore()).isTrue();
        verify(items).findByCompany_ComIdAndSheet_SheetIdInOrderByEmployee_NameAsc(100L, List.of(10L));
    }

    @Test
    void shouldKeepCompanyScopeUnchangedWithoutStoreFilter() {
        Company company = company();
        PayrollSheet sheet = sheet(10L, company, store(11L, "总店"), "2029-04");
        PayrollItemRepository items = mock(PayrollItemRepository.class);

        PayrollSheetScopeContext context = new PayrollSheetScopeContext(items);
        Page<PayrollSheet> page = new PageImpl<>(List.of(sheet), PageRequest.of(0, 20), 1);

        assertThat(context.scopePage(100L, page, null)).isSameAs(page);
    }

    private PayrollSheet sheet(Long id, Company company, Store store, String period) {
        PayrollSheet sheet = new PayrollSheet();
        sheet.setSheetId(id);
        sheet.setCompany(company);
        sheet.setStore(store);
        sheet.setPeriod(period);
        return sheet;
    }

    private PayrollItem item(Long id, Company company, Store store, PayrollSheet sheet,
                             String gross, String deduction, String net) {
        PayrollItem item = new PayrollItem();
        item.setItemId(id);
        item.setCompany(company);
        item.setStore(store);
        item.setSheet(sheet);
        item.setGrossAmount(new BigDecimal(gross));
        item.setDeductionAmount(new BigDecimal(deduction));
        item.setNetAmount(new BigDecimal(net));
        return item;
    }

    private Company company() {
        Company company = new Company();
        company.setComId(100L);
        return company;
    }

    private Store store(Long id, String name) {
        Store store = new Store();
        store.setStoreId(id);
        store.setName(name);
        return store;
    }
}
