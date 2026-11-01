package com.th4.edge.decoboon.supply;

import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.decoboon.supply.service.partnerprice.page.support.Page;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 采购价编辑回显必须按 ID 精确读取，不能先拉全量再查找。 */
class PartnerPricePageTest {

    @Test
    void partnerPriceInfoLookupUsesScopedPagination() {
        PartnerPriceRepository repository = mock(PartnerPriceRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        Employee finance = employee();
        when(tenant.requireRole(eq(finance), any(EmployeeRole[].class))).thenReturn(finance);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<PartnerPrice>(List.of(), invocation.getArgument(1), 0));

        Page provider = new Page(tenant, repository);
        ReflectionTestUtils.setField(provider, "employee", finance);
        ReflectionTestUtils.setField(provider, "partnerPriceId", 99L);
        ReflectionTestUtils.setField(provider, "current", 1);
        ReflectionTestUtils.setField(provider, "size", 200);

        assertThat(provider.call(new PartnerPrice()).getTotalElements()).isZero();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }

    private static Employee employee() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(9L);
        employee.setRole(EmployeeRole.Finance);
        employee.setCompany(company);
        return employee;
    }
}
