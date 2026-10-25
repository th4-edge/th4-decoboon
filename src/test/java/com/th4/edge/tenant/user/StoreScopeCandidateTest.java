package com.th4.edge.tenant.user;

import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-081：门店候选由数据范围决定，不再仅按 BOSS 角色判断。 */
class StoreScopeCandidateTest {

    @Test
    void headquartersFinanceGetsCompanyStoresThroughDataScope() {
        TenantContext tenant = mock(TenantContext.class);
        DataScopeContext scopes = mock(DataScopeContext.class);
        StoreRepository stores = mock(StoreRepository.class);
        Employee finance = employee(1L, EmployeeRole.Finance);
        Store head = store(10L, "总店", true);
        Store second = store(20L, "二店", false);
        finance.setStore(head);
        when(tenant.requireEmployee(finance)).thenReturn(finance);
        when(scopes.resolve(finance)).thenReturn(new DataScope(DataScopeType.Company, 100L, 10L, null, 1L));
        when(stores.findByCompany_ComIdOrderBySortAscStoreIdAsc(100L)).thenReturn(List.of(head, second));

        var provider = new com.th4.edge.tenant.user.service.user.store.list.support.List(tenant, scopes, stores);
        ReflectionTestUtils.setField(provider, "employee", finance);

        assertThat(provider.call(null)).containsExactly(head, second);
    }

    @Test
    void storeFinanceAndManagerOnlyGetOwnStore() {
        TenantContext tenant = mock(TenantContext.class);
        DataScopeContext scopes = mock(DataScopeContext.class);
        StoreRepository stores = mock(StoreRepository.class);
        Employee finance = employee(2L, EmployeeRole.Finance);
        Store second = store(20L, "二店", false);
        finance.setStore(second);
        when(tenant.requireEmployee(finance)).thenReturn(finance);
        when(scopes.resolve(finance)).thenReturn(new DataScope(DataScopeType.Store, 100L, 20L, null, 2L));

        var provider = new com.th4.edge.tenant.user.service.user.store.list.support.List(tenant, scopes, stores);
        ReflectionTestUtils.setField(provider, "employee", finance);

        assertThat(provider.call(null)).containsExactly(second);
    }

    private Employee employee(Long id, EmployeeRole role) {
        Employee employee = new Employee();
        ReflectionTestUtils.setField(employee, "empId", id);
        employee.setRole(role);
        return employee;
    }

    private Store store(Long id, String name, boolean defaultStore) {
        Store store = new Store();
        ReflectionTestUtils.setField(store, "storeId", id);
        store.setName(name);
        store.setDefaultStore(defaultStore);
        store.setCompany(new Company());
        return store;
    }
}
