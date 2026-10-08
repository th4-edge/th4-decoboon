package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerRiskLevel;
import com.th4.edge.decoboon.customer.context.CustomerInsightContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跟进逾期口径回归：只在客户预约量房之前成立，预约量房后不再贯穿整个生命周期。
 */
class CustomerInsightContextTest {

    private final CustomerInsightContext insight = new CustomerInsightContext();

    @Test
    void overdueOnlyHoldsBeforeMeasureBooked() {
        LocalDate today = LocalDate.now();
        LocalDateTime overdueAt = today.minusDays(2).atTime(9, 0);

        assertThat(insight.overdue(customer(CustStatus.New, overdueAt), today)).isTrue();
        assertThat(insight.overdue(customer(CustStatus.Following, overdueAt), today)).isTrue();
        // 待量房即已预约量房，此后不再判定跟进逾期
        assertThat(insight.overdue(customer(CustStatus.PendingMeasure, overdueAt), today)).isFalse();
        assertThat(insight.overdue(customer(CustStatus.Quoted, overdueAt), today)).isFalse();
        assertThat(insight.overdue(customer(CustStatus.Signed, overdueAt), today)).isFalse();
        assertThat(insight.overdue(customer(CustStatus.Serving, overdueAt), today)).isFalse();
    }

    @Test
    void overdueSuppressedWhenMeasureBookedFactExistsDespiteFollowingStatus() {
        LocalDate today = LocalDate.now();
        Customer following = customer(CustStatus.Following, today.minusDays(3).atTime(9, 0));
        // 历史/异常数据兜底：状态仍停在跟进中，但已写入约定量房时间，同样不再判跟进逾期。
        following.setMeasureAt(today.plusDays(1).atTime(10, 0));
        assertThat(CustomerInsightContext.followOverdueEligible(following)).isFalse();
        assertThat(insight.overdue(following, today)).isFalse();

        Customer requested = customer(CustStatus.Following, today.minusDays(3).atTime(9, 0));
        requested.setMeasureRequestedAt(today.minusDays(1).atTime(10, 0));
        assertThat(insight.overdue(requested, today)).isFalse();

        Customer pending = customer(CustStatus.Following, today.minusDays(3).atTime(9, 0));
        pending.setPendingMeasureAt(today.minusDays(1).atTime(10, 0));
        assertThat(insight.overdue(pending, today)).isFalse();
    }

    @Test
    void overdueRiskNoLongerRaisedAfterMeasureBooked() {
        LocalDate today = LocalDate.now();
        LocalDateTime overdueAt = today.minusDays(5).atTime(9, 0);
        Employee owner = employee(1L);
        Employee designer = employee(2L);

        Customer following = customer(CustStatus.Following, overdueAt);
        following.setOwner(owner);
        insight.riskAndAction(following, 1, today);
        assertThat(following.getRiskReason()).contains("跟进已逾期");
        assertThat(following.getNextAction()).contains("逾期跟进");

        Customer pendingMeasure = customer(CustStatus.PendingMeasure, overdueAt);
        pendingMeasure.setOwner(owner);
        pendingMeasure.setDesigner(designer);
        insight.riskAndAction(pendingMeasure, 1, today);
        assertThat(pendingMeasure.getRiskLevel()).isEqualTo(CustomerRiskLevel.Normal);
        assertThat(pendingMeasure.getRiskReason()).isNull();
        assertThat(pendingMeasure.getNextAction()).doesNotContain("逾期跟进");
    }

    private Customer customer(CustStatus status, LocalDateTime nextFollowAt) {
        Customer customer = new Customer();
        customer.setCustId(1L);
        customer.setStatus(status);
        customer.setName("测试客户");
        customer.setNextFollowAt(nextFollowAt);
        return customer;
    }

    private Employee employee(Long id) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        return employee;
    }
}
