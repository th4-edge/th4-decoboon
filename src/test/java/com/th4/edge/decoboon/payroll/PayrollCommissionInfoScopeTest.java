package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.service.commissions.info.support.Info;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayrollCommissionInfoScopeTest {

    @Test
    void shouldResolveCommissionThroughDataScopeSpecification() {
        CommissionItemRepository repository = mock(CommissionItemRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        Employee current = employee(1L);
        CommissionItem item = commission(9L);
        @SuppressWarnings("unchecked")
        Specification<CommissionItem> visible = mock(Specification.class);
        when(access.requireCommissionViewer(current)).thenReturn(current);
        when(access.visible(current, CommissionItem.class)).thenReturn(visible);
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(item));

        Info provider = new Info(repository, access);
        ReflectionTestUtils.setField(provider, "employee", current);
        ReflectionTestUtils.setField(provider, "commissionId", 9L);

        assertThat(provider.call(new CommissionItem())).isSameAs(item);
        verify(access).visible(current, CommissionItem.class);
        verify(repository).findOne(any(Specification.class));
        verify(repository, never()).findById(any());
    }

    @Test
    void shouldHideCommissionWhenScopedLookupDoesNotMatch() {
        CommissionItemRepository repository = mock(CommissionItemRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        Employee current = employee(1L);
        @SuppressWarnings("unchecked")
        Specification<CommissionItem> visible = mock(Specification.class);
        when(access.requireCommissionViewer(current)).thenReturn(current);
        when(access.visible(current, CommissionItem.class)).thenReturn(visible);
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        Info provider = new Info(repository, access);
        ReflectionTestUtils.setField(provider, "employee", current);
        ReflectionTestUtils.setField(provider, "commissionId", 9L);

        assertThatThrownBy(() -> provider.call(new CommissionItem()))
                .hasMessageContaining("提成不存在或无权访问");
    }

    private Employee employee(Long empId) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        return employee;
    }

    private CommissionItem commission(Long commissionId) {
        CommissionItem item = new CommissionItem();
        item.setCommissionId(commissionId);
        return item;
    }
}
