package com.th4.edge.decoboon.carryover;

import com.th4.edge.decoboon.carryover.configure.CarryoverPaymentDirection;
import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.context.CarryoverPaymentPlanContext;
import com.th4.edge.decoboon.carryover.model.CarryoverPaymentPlan;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverPaymentPlanRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 剩余收付款计划必须是对期初余额的时间分解，不能超过期初余额。 */
class CarryoverPaymentPlanContextTest {

    private final CarryoverAccessContext accessContext = mock(CarryoverAccessContext.class);
    private final CarryoverPaymentPlanRepository repository = mock(CarryoverPaymentPlanRepository.class);
    private final CarryoverPaymentPlanContext context =
            new CarryoverPaymentPlanContext(accessContext, repository);

    @Test
    void planMustNotExceedOutstanding() {
        Employee boss = boss();
        CarryoverProject item = item(boss, "120000.00", "40000.00");
        when(accessContext.requireFinanceEditableItem(boss, 1L)).thenReturn(item);

        assertThatThrownBy(() -> context.save(boss, 1L, List.of(plan(CarryoverPaymentDirection.Receivable, "130000.00"))))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("收款计划合计不能超过期初应收");
    }

    @Test
    void unplannedRemainderIsReported() {
        Employee boss = boss();
        CarryoverProject item = item(boss, "120000.00", "40000.00");
        when(accessContext.requireFinanceEditableItem(boss, 1L)).thenReturn(item);
        when(repository.save(any(CarryoverPaymentPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var report = context.save(boss, 1L, List.of(
                plan(CarryoverPaymentDirection.Receivable, "100000.00"),
                plan(CarryoverPaymentDirection.Payable, "20000.00")));

        assertThat(report.getPlannedReceivable()).isEqualByComparingTo("100000.00");
        assertThat(report.getPlannedPayable()).isEqualByComparingTo("20000.00");
        assertThat(report.getUnplannedReceivable()).isEqualByComparingTo("20000.00");
        assertThat(report.getUnplannedPayable()).isEqualByComparingTo("20000.00");
        verify(repository).deleteByCompany_ComIdAndItem_ItemId(1L, 1L);
    }

    private CarryoverPaymentPlan plan(CarryoverPaymentDirection direction, String amount) {
        CarryoverPaymentPlan plan = new CarryoverPaymentPlan();
        plan.setDirection(direction);
        plan.setAmount(new BigDecimal(amount));
        return plan;
    }

    private CarryoverProject item(Employee owner, String receivable, String payable) {
        CarryoverProject item = new CarryoverProject();
        item.setItemId(1L);
        item.setCompany(owner.getCompany());
        item.setOwner(owner);
        item.setReceivableOutstanding(new BigDecimal(receivable));
        item.setPayableOutstanding(new BigDecimal(payable));
        return item;
    }

    private Employee boss() {
        Company company = new Company();
        company.setComId(1L);
        Employee boss = new Employee();
        boss.setEmpId(1L);
        boss.setCompany(company);
        boss.setRole(EmployeeRole.Boss);
        return boss;
    }
}
