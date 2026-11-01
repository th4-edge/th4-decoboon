package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 工资单详情发放只能看到当前工资单来源的员工应付。 */
class PayrollEmployeePaymentPageScopeTest {

    @Test
    void sheetContextFiltersOutOtherSheetAndCommissionPayables() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);

        Payable currentSheetPayable = payable(11L, PayableType.EmployeeSalary, "PayrollItem", 101L);
        PayrollItem currentSheetItem = new PayrollItem();
        currentSheetItem.setItemId(101L);

        PayableRepository payableRepository = mock(PayableRepository.class);
        FinanceAccessContext financeAccess = mock(FinanceAccessContext.class);
        PayrollAccessContext payrollAccess = mock(PayrollAccessContext.class);
        StoreFilterContext storeFilter = mock(StoreFilterContext.class);
        PayrollItemRepository payrollItemRepository = mock(PayrollItemRepository.class);
        when(payrollAccess.requirePaymentActor(boss)).thenReturn(boss);
        when(storeFilter.resolve(boss, null)).thenReturn(0L);
        when(financeAccess.visible(eq(boss), eq(Payable.class)))
                .thenReturn((Specification<Payable>) (root, query, cb) -> cb.conjunction());
        when(payableRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(currentSheetPayable)));
        when(payableRepository.findDetailsByIdIn(any()))
                .thenReturn(List.of(currentSheetPayable));
        when(payrollItemRepository.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(100L, 9L))
                .thenReturn(List.of(currentSheetItem));

        com.th4.edge.decoboon.finance.service.payments.employee.page.support.Page provider =
                new com.th4.edge.decoboon.finance.service.payments.employee.page.support.Page(
                        payableRepository, financeAccess, payrollAccess, storeFilter, payrollItemRepository);
        ReflectionTestUtils.setField(provider, "employee", boss);
        ReflectionTestUtils.setField(provider, "sheetId", 9L);

        var result = provider.call(new Payable());

        assertThat(result.getContent()).extracting(Payable::getPayableId).containsExactly(11L);
        verify(payableRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(payableRepository).findDetailsByIdIn(any());
    }

    private Payable payable(Long id, PayableType type, String sourceType, Long sourceId) {
        Payable payable = new Payable();
        payable.setPayableId(id);
        payable.setType(type);
        payable.setSourceType(sourceType);
        payable.setSourceId(sourceId);
        payable.setAmount(new BigDecimal("100.00"));
        payable.setPaidAmount(BigDecimal.ZERO);
        return payable;
    }
}
