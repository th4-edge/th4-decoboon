package com.th4.edge.decoboon.ai;

import com.th4.edge.decoboon.ai.configure.AiBillingStatus;
import com.th4.edge.decoboon.ai.configure.AiStoreAllocation;
import com.th4.edge.decoboon.ai.configure.AiUsageStatus;
import com.th4.edge.decoboon.ai.context.AiUsageContext;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.ai.repository.AiUsageCallRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiUsageContextTest {

    private static final long COM_ID = 41L;

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final AiUsageCallRepository repository = mock(AiUsageCallRepository.class);
    private final Map<String, AiUsageCall> stored = new LinkedHashMap<>();
    private final AiUsageContext context = new AiUsageContext(tenantContext, repository);

    private Employee boss;
    private Store employeeStore;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(COM_ID);
        boss = employee(1L, "周总", company, null);
        employeeStore = store(10L, "总店", company);
        boss.setStore(employeeStore);
        when(tenantContext.requireEmployee(any())).thenReturn(boss);
        when(repository.findByCompany_ComIdAndIdempotencyKey(any(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(1);
            return Optional.ofNullable(stored.get(key));
        });
        when(repository.save(any(AiUsageCall.class))).thenAnswer(invocation -> {
            AiUsageCall call = invocation.getArgument(0);
            stored.put(call.getIdempotencyKey(), call);
            return call;
        });
    }

    @Test
    void businessStoreShouldWinAndShadowUnitsShouldBeCalculated() {
        Store businessStore = store(20L, "滨湖店", boss.getCompany());

        AiUsageCall request = request("quote-check-1");
        request.setBusinessStore(businessStore);
        request.setInputUnits(120L);
        request.setOutputUnits(80L);

        AiUsageCall saved = context.record(boss, request);

        assertThat(saved.getCompany().getComId()).isEqualTo(COM_ID);
        assertThat(saved.getAllocationStore()).isSameAs(businessStore);
        assertThat(saved.getAllocationType()).isEqualTo(AiStoreAllocation.BusinessStore);
        assertThat(saved.getResourceUnits()).isEqualTo(200L);
        assertThat(saved.getShadowBillableUnits()).isEqualTo(200L);
        assertThat(saved.getBillingStatus()).isEqualTo(AiBillingStatus.Calculated);
        assertThat(saved.getRateCardVersion()).isEqualTo("shadow-direct-v1");
    }

    @Test
    void employeeStoreShouldBeUsedWhenBusinessStoreIsMissing() {
        AiUsageCall saved = context.record(boss, request("customer-summary-1"));

        assertThat(saved.getAllocationStore()).isSameAs(employeeStore);
        assertThat(saved.getAllocationType()).isEqualTo(AiStoreAllocation.EmployeeStore);
    }

    @Test
    void enterprisePublicShouldBeUsedWhenNoStoreCanBeResolved() {
        boss.setStore(null);
        AiUsageCall saved = context.record(boss, request("boss-brief-1"));

        assertThat(saved.getAllocationStore()).isNull();
        assertThat(saved.getAllocationType()).isEqualTo(AiStoreAllocation.EnterprisePublic);
        assertThat(saved.getAllocationRuleVersion()).isEqualTo("store-allocation-v1");
    }

    @Test
    void sameIdempotencyKeyShouldReturnExistingRecord() {
        AiUsageCall first = context.record(boss, request("same-action"));
        AiUsageCall second = context.record(boss, request("same-action"));

        assertThat(second).isSameAs(first);
        assertThat(stored).hasSize(1);
    }

    @Test
    void crossCompanyStoreShouldBeRejected() {
        Company other = new Company();
        other.setComId(99L);
        AiUsageCall request = request("cross-company");
        request.setBusinessStore(store(90L, "外企门店", other));

        assertThatThrownBy(() -> context.record(boss, request))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能关联其他企业的门店");
    }

    private AiUsageCall request(String key) {
        AiUsageCall call = new AiUsageCall();
        call.setIdempotencyKey(key);
        call.setBusinessModule("Analytics");
        call.setCapabilityCode("boss-brief");
        call.setStatus(AiUsageStatus.Succeeded);
        call.setStartedAt(java.time.LocalDateTime.now().minusSeconds(2));
        call.setCompletedAt(java.time.LocalDateTime.now());
        return call;
    }

    private Employee employee(Long id, String name, Company company, Store store) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setName(name);
        employee.setRole(EmployeeRole.Boss);
        employee.setCompany(company);
        employee.setStore(store);
        return employee;
    }

    private Store store(Long id, String name, Company company) {
        Store store = new Store();
        store.setStoreId(id);
        store.setName(name);
        store.setCompany(company);
        return store;
    }
}
