package com.th4.edge.tenant.user;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Setting;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.tenant.user.service.user.store.save.support.Save;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-081：企业创建后的新门店永远不是默认总店。 */
class StoreSaveDefaultStoreTest {

    @Test
    void newStoreIsAlwaysNonDefault() {
        TenantContext tenant = mock(TenantContext.class);
        SubscriptionContext subscription = mock(SubscriptionContext.class);
        DistributedLock lock = mock(DistributedLock.class);
        StoreRepository stores = mock(StoreRepository.class);
        CompanyRepository companies = mock(CompanyRepository.class);
        ModelIdentityContext identities = mock(ModelIdentityContext.class);

        Company company = new Company();
        company.setComId(1L);
        company.setSetting(new Setting());
        Employee boss = new Employee();
        boss.setCompany(company);
        when(tenant.requireBoss(any())).thenReturn(boss);
        when(lock.tryLock(any(), any(Long.class), any(Long.class))).thenReturn(true);
        when(stores.countByCompany_ComId(1L)).thenReturn(1L);
        when(stores.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(identities.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Save save = new Save(tenant, subscription, lock, stores, companies, identities);
        ReflectionTestUtils.setField(save, "name", "二店");
        ReflectionTestUtils.setField(save, "color", "#000000");
        ReflectionTestUtils.setField(save, "addr", "测试地址");
        ReflectionTestUtils.setField(save, "employee", boss);

        Store saved = save.call(new Store());

        assertThat(saved.getDefaultStore()).isFalse();
        assertThat(saved.getName()).isEqualTo("二店");
    }
}
