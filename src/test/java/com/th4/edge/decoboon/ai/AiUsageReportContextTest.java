package com.th4.edge.decoboon.ai;

import com.th4.edge.decoboon.ai.configure.AiBillingStatus;
import com.th4.edge.decoboon.ai.configure.AiStoreAllocation;
import com.th4.edge.decoboon.ai.configure.AiUsageStatus;
import com.th4.edge.decoboon.ai.context.AiUsageReportContext;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.ai.model.AiUsageQuery;
import com.th4.edge.decoboon.ai.model.AiUsageReport;
import com.th4.edge.decoboon.ai.repository.AiUsageCallRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiUsageReportContextTest {

    private static final long COM_ID = 41L;

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final AiUsageCallRepository repository = mock(AiUsageCallRepository.class);
    private final AiUsageReportContext context = new AiUsageReportContext(tenantContext, repository);

    private Employee boss;
    private Store store;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(COM_ID);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        store = new Store();
        store.setStoreId(10L);
        store.setName("总店");
        store.setCompany(company);
        when(tenantContext.requireBoss(boss)).thenReturn(boss);
    }

    @Test
    void reportShouldAggregateCompanyStoreAndEnterprisePublic() {
        LocalDate today = LocalDate.now();
        AiUsageCall storeCall = call(1L, store, AiUsageStatus.Succeeded, 100L, 10L, today);
        AiUsageCall failedStoreCall = call(2L, store, AiUsageStatus.Failed, 0L, 0L, today);
        AiUsageCall publicCall = call(3L, null, AiUsageStatus.Succeeded, 20L, 2L, today);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(storeCall, failedStoreCall, publicCall));

        AiUsageReport report = context.report(boss, new AiUsageQuery());

        assertThat(report.getRequestCount()).isEqualTo(3L);
        assertThat(report.getValidCallCount()).isEqualTo(2L);
        assertThat(report.getSuccessCount()).isEqualTo(2L);
        assertThat(report.getFailureCount()).isEqualTo(1L);
        assertThat(report.getResourceUnits()).isEqualTo(120L);
        assertThat(report.getShadowBillableUnits()).isEqualTo(12L);
        assertThat(report.getSuccessRate()).isEqualByComparingTo("100.0");
        assertThat(report.getStores()).hasSize(2);
        assertThat(report.getStores()).anySatisfy(item -> {
            assertThat(item.getStoreName()).isEqualTo("企业公共");
            assertThat(item.getAllocationType()).isEqualTo(AiStoreAllocation.EnterprisePublic);
        });
        assertThat(report.getCapabilities()).singleElement()
                .satisfies(item -> assertThat(item.getShadowBillableUnits()).isEqualTo(12L));
    }

    @Test
    void pageShouldClampSizeAndReturnRepositoryPage() {
        when(repository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AiUsageQuery query = new AiUsageQuery();
        query.setSize(999);

        assertThat(context.page(boss, query).getTotalElements()).isZero();
    }

    private AiUsageCall call(Long id, Store allocationStore, AiUsageStatus status,
                             Long resourceUnits, Long shadowUnits, LocalDate date) {
        AiUsageCall call = new AiUsageCall();
        call.setCallId(id);
        call.setCompany(boss.getCompany());
        call.setAllocationStore(allocationStore);
        call.setAllocationType(allocationStore == null
                ? AiStoreAllocation.EnterprisePublic : AiStoreAllocation.BusinessStore);
        call.setCapabilityCode("boss-brief");
        call.setStatus(status);
        call.setBillingStatus(status == AiUsageStatus.Succeeded ? AiBillingStatus.Calculated : AiBillingStatus.Void);
        call.setResourceUnits(resourceUnits);
        call.setShadowBillableUnits(shadowUnits);
        call.setRefundedUnits(0L);
        call.setStartedAt(LocalDateTime.of(date, java.time.LocalTime.NOON));
        return call;
    }
}
