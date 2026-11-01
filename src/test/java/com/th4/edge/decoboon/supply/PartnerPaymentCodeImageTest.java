package com.th4.edge.decoboon.supply;

import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.decoboon.supply.service.partner.update.support.Update;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PartnerPaymentCodeImageTest {

    @Test
    void shouldPersistOrderedPaymentCodeImagesOnSupplierUpdate() {
        TenantContext tenantContext = mock(TenantContext.class);
        PartnerRepository repository = mock(PartnerRepository.class);
        Company company = new Company();
        company.setComId(10L);
        Employee finance = new Employee();
        finance.setRole(EmployeeRole.Finance);
        finance.setCompany(company);
        Partner partner = new Partner();
        partner.setPartnerId(20L);
        partner.setCompany(company);
        partner.setName("测试供应商");

        when(tenantContext.requireRole(finance, EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Finance))
                .thenReturn(finance);
        when(repository.findById(20L)).thenReturn(Optional.of(partner));
        when(repository.save(any(Partner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Update update = new Update(tenantContext, repository);
        ReflectionTestUtils.setField(update, "employee", finance);
        ReflectionTestUtils.setField(update, "partnerId", 20L);
        ReflectionTestUtils.setField(update, "paymentCodeImages",
                List.of("companies/10/files/partner/code-1.png", "companies/10/files/partner/code-2.jpg"));

        Partner saved = update.call(new Partner());

        assertThat(saved.getPaymentCodeImages()).containsExactly(
                "companies/10/files/partner/code-1.png",
                "companies/10/files/partner/code-2.jpg");
    }
}
