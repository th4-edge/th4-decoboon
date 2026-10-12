package com.th4.edge.decoboon.common.access;

import com.th4.edge.decoboon.common.context.StoreAttributionContext;
import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 门店归属**解析器**规则单测（`DEC-070`）：负责人无门店时的「单门店默认 / 多门店阻断」、
 * 越权门店校验、无项目手工流水的「操作人门店 → 当前门店维度 → 阻断」。
 *
 * <p>库内 {@code db_tenant_employee.store_id} 为 NOT NULL，实体层无法表达「负责人无门店」，
 * 因此该分支（规格 §4-4）在本类以 mock 覆盖，DB 侧行为由
 * {@link StoreAttributionWriteAcceptanceTest} 覆盖。</p>
 */
class StoreAttributionContextTest {

    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final StoreFilterContext storeFilterContext = mock(StoreFilterContext.class);
    private final StoreAttributionContext context = new StoreAttributionContext(storeRepository, storeFilterContext);

    @Test
    void ownerWithoutStoreBlocksForMultiStoreCompany() {
        Company company = company(1L);
        Employee owner = employee(2L, company, null);
        when(storeRepository.findByCompany_ComId(1L)).thenReturn(List.of(store(10L, company), store(11L, company)));

        assertThatThrownBy(() -> context.forCustomerOwner(owner))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("负责人未归属门店");
    }

    @Test
    void ownerWithoutStoreDefaultsToTheOnlyStoreForSingleStoreCompany() {
        Company company = company(1L);
        Employee owner = employee(2L, company, null);
        when(storeRepository.findByCompany_ComId(1L)).thenReturn(List.of(store(10L, company)));

        assertThat(context.forCustomerOwner(owner).store().getStoreId()).isEqualTo(10L);
    }

    @Test
    void attachedStoreOfAnotherCompanyIsRejected() {
        Company company = company(1L);
        Company other = company(9L);
        Employee owner = employee(2L, company, store(10L, other));

        assertThatThrownBy(() -> context.forCustomerOwner(owner))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("门店不存在或无权访问");
    }

    @Test
    void unknownStoreIsRejectedByRequireCompanyStore() {
        when(storeRepository.findByStoreId(anyLong())).thenReturn(null);
        assertThatThrownBy(() -> context.requireCompanyStore(99L, 1L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("门店不存在或无权访问");
    }

    @Test
    void operatorWithoutStoreUsesScopeStoreThenBlocks() {
        Company company = company(1L);
        Employee operator = employee(2L, company, null);
        Store scopeStore = store(10L, company);
        when(storeFilterContext.resolve(eq(operator), any())).thenReturn(0L);
        assertThatThrownBy(() -> context.forOperator(operator, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("未归属门店");

        when(storeFilterContext.resolve(eq(operator), any())).thenReturn(10L);
        when(storeRepository.findByStoreId(10L)).thenReturn(scopeStore);
        assertThat(context.forOperator(operator, null).store().getStoreId()).isEqualTo(10L);
        assertThat(context.forOperator(operator, null).source()).isEqualTo(StoreAttributionContext.SCOPE_STORE);
    }

    @Test
    void customerSnapshotPrefersCustomerStoreOverOwnerStore() {
        Company company = company(1L);
        Store customerStore = store(10L, company);
        Store ownerStore = store(11L, company);
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(customerStore);
        customer.setOwner(employee(2L, company, ownerStore));

        StoreAttributionContext.AttributedStore attributed = context.forCustomerSnapshot(customer, null, null);
        assertThat(attributed.store().getStoreId()).isEqualTo(10L);
        assertThat(attributed.source()).isEqualTo(StoreAttributionContext.CUSTOMER_STORE);
    }

    @Test
    void legacyCustomerWithoutResolvableStoreFallsBackInsteadOfBlocking() {
        Company company = company(1L);
        Customer customer = new Customer();
        customer.setCompany(company);
        when(storeRepository.findByCompany_ComId(1L)).thenReturn(List.of(store(10L, company), store(11L, company)));
        Store legacy = store(11L, company);

        // 存量客户无门店 + 多门店企业 + 负责人无门店 → 不阻断，按历史来源回落并告警（进存量清单）
        assertThat(context.forCustomerSnapshot(customer, null, legacy).store().getStoreId()).isEqualTo(11L);
    }

    // ---------- 夹具 ----------

    private Company company(Long comId) {
        Company company = new Company();
        company.setComId(comId);
        return company;
    }

    private Store store(Long storeId, Company company) {
        Store store = new Store();
        store.setStoreId(storeId);
        store.setCompany(company);
        store.setName("门店" + storeId);
        return store;
    }

    private Employee employee(Long empId, Company company, Store store) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setCompany(company);
        employee.setStore(store);
        return employee;
    }
}
