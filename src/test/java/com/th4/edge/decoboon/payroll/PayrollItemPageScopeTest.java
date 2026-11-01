package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-098：门店财务只能读取本店员工的工资项。 */
class PayrollItemPageScopeTest {

    @Test
    void storeFinanceOnlySeesOwnStorePayrollItems() {
        Employee finance = new Employee();
        finance.setEmpId(1L);
        finance.setRole(EmployeeRole.Finance);

        PayrollItem own = item(10L, 101L);
        PayrollItem other = item(20L, 201L);
        PayrollItemRepository repository = mock(PayrollItemRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        StoreFilterContext storeFilter = mock(StoreFilterContext.class);
        when(access.requireManager(finance)).thenReturn(finance);
        when(access.visible(finance, PayrollItem.class)).thenReturn(conjunction());
        when(storeFilter.resolve(finance, null)).thenReturn(10L);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(own, other));

        var page = new com.th4.edge.decoboon.payroll.service.items.page.support.Page(repository, access);
        ReflectionTestUtils.setField(page, "employee", finance);
        ReflectionTestUtils.setField(page, "storeFilterContext", storeFilter);

        var result = page.call(new PayrollItem());

        assertThat(result.getContent()).extracting(PayrollItem::getItemId).containsExactly(101L);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    private Specification<PayrollItem> conjunction() {
        return (root, query, cb) -> cb.conjunction();
    }

    private PayrollItem item(Long storeId, Long itemId) {
        Store store = new Store();
        store.setStoreId(storeId);
        PayrollItem item = new PayrollItem();
        item.setItemId(itemId);
        item.setStore(store);
        return item;
    }
}
