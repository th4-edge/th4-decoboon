package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerDormantReason;
import com.th4.edge.decoboon.customer.context.CustomerDormantContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerStatusLog;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.repository.CustomerStatusLogRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerDormantContextTest {

    private CustomerRepository customerRepository;
    private CustomerStatusLogRepository logRepository;
    private MeasureRepository measureRepository;
    private ProjectContractRepository contractRepository;
    private CustomerDormantContext context;
    private Employee marketer;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        logRepository = mock(CustomerStatusLogRepository.class);
        measureRepository = mock(MeasureRepository.class);
        contractRepository = mock(ProjectContractRepository.class);
        context = new CustomerDormantContext(customerRepository, logRepository, measureRepository, contractRepository);

        Company company = new Company();
        company.setComId(10L);
        marketer = new Employee();
        marketer.setEmpId(20L);
        marketer.setRole(EmployeeRole.Marketer);
        marketer.setCompany(company);

        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(logRepository.save(any(CustomerStatusLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(measureRepository.existsByCompany_ComIdAndCustomer_CustIdAndStatusIn(
                anyLong(), anyLong(), any(Collection.class))).thenReturn(false);
        when(contractRepository.existsByCompany_ComIdAndCustomer_CustIdAndStatusNotIn(
                anyLong(), anyLong(), any(Collection.class))).thenReturn(false);
    }

    @Test
    void shouldSetDormantAndWriteStatusLog() {
        Customer customer = customer(CustStatus.Following);
        customer.setNextFollowAt(LocalDateTime.now().plusDays(1));

        Customer saved = context.setDormant(customer, marketer, CustomerDormantReason.PlanPostponed,
                "计划延期", CustStatus.Following, "req-1");

        assertThat(saved.getStatus()).isEqualTo(CustStatus.Dormant);
        assertThat(saved.getStatusBeforeDormant()).isEqualTo(CustStatus.Following);
        assertThat(saved.getNextFollowAt()).isNull();
        assertThat(saved.getDormantAt()).isNotNull();

        ArgumentCaptor<CustomerStatusLog> captor = ArgumentCaptor.forClass(CustomerStatusLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(CustStatus.Following);
        assertThat(captor.getValue().getToStatus()).isEqualTo(CustStatus.Dormant);
        assertThat(captor.getValue().getReasonCode()).isEqualTo(CustomerDormantReason.PlanPostponed);
    }

    @Test
    void shouldRejectMissingReason() {
        assertThatThrownBy(() -> context.setDormant(customer(CustStatus.Following), marketer,
                null, null, CustStatus.Following, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("请选择休眠原因");
    }

    @Test
    void shouldRejectOtherReasonWithoutDetail() {
        assertThatThrownBy(() -> context.setDormant(customer(CustStatus.Following), marketer,
                CustomerDormantReason.Other, " ", CustStatus.Following, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("必须填写");
    }

    @Test
    void shouldBlockWhenFutureMeasureExists() {
        when(measureRepository.existsByCompany_ComIdAndCustomer_CustIdAndStatusIn(
                anyLong(), anyLong(), any(Collection.class))).thenReturn(true);

        assertThatThrownBy(() -> context.setDormant(customer(CustStatus.PendingMeasure), marketer,
                CustomerDormantReason.PlanPostponed, null, CustStatus.PendingMeasure, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("未完成量房预约");
    }

    @Test
    void shouldRestorePreviousStatus() {
        Customer customer = customer(CustStatus.Dormant);
        customer.setStatusBeforeDormant(CustStatus.Following);

        Customer restored = context.restore(customer, marketer, null,
                LocalDateTime.now().plusDays(3), "req-2");

        assertThat(restored.getStatus()).isEqualTo(CustStatus.Following);
        assertThat(restored.getStatusBeforeDormant()).isNull();
        assertThat(restored.getNextFollowAt()).isNotNull();
        assertThat(restored.getRestoredAt()).isNotNull();
    }

    @Test
    void shouldWakeDormantCustomerWhenFollowUpIsSaved() {
        Customer customer = customer(CustStatus.Dormant);
        customer.setStatusBeforeDormant(CustStatus.Quoted);

        Customer restored = context.wakeByFollow(customer, marketer);

        assertThat(restored.getStatus()).isEqualTo(CustStatus.Quoted);
        assertThat(restored.getStatusBeforeDormant()).isNull();
    }

    @Test
    void shouldRejectDesignerRole() {
        Employee designer = new Employee();
        designer.setEmpId(21L);
        designer.setRole(EmployeeRole.Designer);
        designer.setCompany(marketer.getCompany());

        assertThatThrownBy(() -> context.setDormant(customer(CustStatus.Following), designer,
                CustomerDormantReason.PlanPostponed, null, CustStatus.Following, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("仅业务员、店长或企业负责人");
    }

    private Customer customer(CustStatus status) {
        Customer customer = new Customer();
        customer.setCustId(101L);
        customer.setCompany(marketer.getCompany());
        customer.setName("测试客户");
        customer.setOwner(marketer);
        customer.setStatus(status);
        return customer;
    }
}
