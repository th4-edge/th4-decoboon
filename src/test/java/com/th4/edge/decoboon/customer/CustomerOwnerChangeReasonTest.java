package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerStatusChangeType;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerStatusLog;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.repository.CustomerStatusLogRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerOwnerChangeReasonTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerContext customerContext;
    @Mock private DataScopeContext dataScopeContext;
    @Mock private CustomerStatusLogRepository statusLogRepository;

    private com.th4.edge.decoboon.customer.service.cust.update.support.Update update;
    private Employee current;
    private Employee previousOwner;
    private Employee nextOwner;
    private Customer customer;

    @BeforeEach
    void setUp() {
        update = new com.th4.edge.decoboon.customer.service.cust.update.support.Update(
                customerRepository, customerContext, dataScopeContext, statusLogRepository);
        Company company = new Company();
        company.setComId(1L);
        current = employee(10L, "张店长", EmployeeRole.StoreManager, company);
        previousOwner = employee(11L, "原负责人", EmployeeRole.Marketer, company);
        nextOwner = employee(12L, "新负责人", EmployeeRole.Designer, company);
        customer = new Customer();
        customer.setCustId(100L);
        customer.setCompany(company);
        customer.setStatus(CustStatus.Following);
        customer.setOwner(previousOwner);

        when(dataScopeContext.requireEmployee(current)).thenReturn(current);
        when(customerContext.requireVisible(100L, current)).thenReturn(customer);
        ReflectionTestUtils.setField(update, "employee", current);
        ReflectionTestUtils.setField(update, "customerId", 100L);
        ReflectionTestUtils.setField(update, "ownerId", nextOwner.getEmpId());
    }

    @Test
    void ownerChangeWithoutReasonIsRejectedBeforeWrite() {
        when(customerContext.resolveOwner(current, nextOwner.getEmpId(), previousOwner))
                .thenReturn(nextOwner);

        assertThatThrownBy(() -> update.call(new Customer()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("必须填写原因");

        verify(customerRepository, never()).save(customer);
        verify(statusLogRepository, never()).save(org.mockito.ArgumentMatchers.any(CustomerStatusLog.class));
    }

    @Test
    void ownerChangeWritesReasonAndAuditLog() {
        when(customerContext.resolveOwner(current, nextOwner.getEmpId(), previousOwner))
                .thenReturn(nextOwner);
        when(customerRepository.save(customer)).thenReturn(customer);
        ReflectionTestUtils.setField(update, "reason", "原负责人离职，转交店内设计师继续服务");

        Customer saved = update.call(new Customer());

        assertThat(saved).isSameAs(customer);
        ArgumentCaptor<CustomerStatusLog> captor = ArgumentCaptor.forClass(CustomerStatusLog.class);
        verify(statusLogRepository).save(captor.capture());
        CustomerStatusLog log = captor.getValue();
        assertThat(log.getChangeType()).isEqualTo(CustomerStatusChangeType.OwnerChange);
        assertThat(log.getReasonText()).isEqualTo("原负责人离职，转交店内设计师继续服务");
        assertThat(log.getFromStatus()).isEqualTo(CustStatus.Following);
        assertThat(log.getToStatus()).isEqualTo(CustStatus.Following);
        assertThat(log.getOperator()).isSameAs(current);
        assertThat(log.getOperatorRole()).isEqualTo(EmployeeRole.StoreManager);
    }

    @Test
    void sameOwnerSubmissionIsIdempotentAndDoesNotWriteAuditLog() {
        when(customerContext.resolveOwner(current, nextOwner.getEmpId(), previousOwner))
                .thenReturn(previousOwner);
        when(customerRepository.save(customer)).thenReturn(customer);

        update.call(new Customer());

        verify(statusLogRepository, never()).save(org.mockito.ArgumentMatchers.any(CustomerStatusLog.class));
    }

    private Employee employee(Long id, String name, EmployeeRole role, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setName(name);
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }
}
