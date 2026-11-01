package com.th4.edge.decoboon.supply;

import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.decoboon.supply.service.partner.page.support.Page;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 供应商选择器使用数据库分页，并批量聚合当前页应付。 */
class PartnerPickerPageTest {

    @Test
    void partnerPickerUsesPaginationAndOnePayableAggregateQuery() {
        PartnerRepository partners = mock(PartnerRepository.class);
        PayableRepository payables = mock(PayableRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        Employee boss = employee();
        Partner partner = new Partner();
        partner.setPartnerId(30L);
        partner.setName("合肥木材供应");
        when(tenant.requireEmployee(boss)).thenReturn(boss);
        when(partners.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(partner), invocation.getArgument(1), 1));
        when(partners.findDetailsByIdIn(eq(1L), eq(List.of(30L)))).thenReturn(List.of(partner));
        when(payables.summarizePartnerPayables(eq(1L), eq(List.of(30L))))
                .thenReturn(List.<Object[]>of(new Object[]{30L, new BigDecimal("1200.00"), new BigDecimal("200.00")}));

        Page provider = new Page(tenant, partners, payables);
        ReflectionTestUtils.setField(provider, "employee", boss);
        ReflectionTestUtils.setField(provider, "current", 1);
        ReflectionTestUtils.setField(provider, "size", 200);
        ReflectionTestUtils.setField(provider, "keyword", "木材");

        var result = provider.call(new Partner());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(partners).findAll(any(Specification.class), pageable.capture());
        verify(payables, times(1)).summarizePartnerPayables(eq(1L), eq(List.of(30L)));
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(result.getContent()).containsExactly(partner);
        assertThat(partner.getOutstandingAmount()).isEqualByComparingTo("1000.00");
    }

    private static Employee employee() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(9L);
        employee.setCompany(company);
        return employee;
    }
}
