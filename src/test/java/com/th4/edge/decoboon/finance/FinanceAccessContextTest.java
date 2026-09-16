package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 财务角色、金额状态与项目归属规则单元测试。 */
class FinanceAccessContextTest {

    private EmployeeRepository employeeRepository;
    private ProjectRepository projectRepository;
    private LedgerRepository ledgerRepository;
    private FinanceAccessContext accessContext;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        projectRepository = mock(ProjectRepository.class);
        ledgerRepository = mock(LedgerRepository.class);
        TenantContext tenantContext = new TenantContext(employeeRepository);
        accessContext = new FinanceAccessContext(
                tenantContext,
                mock(DataScopeContext.class),
                projectRepository,
                ledgerRepository
        );
    }

    @Test
    void shouldAllowFinanceEditButRejectMarketer() {
        Employee finance = employee(EmployeeRole.Finance);
        when(employeeRepository.findByEmpId(finance.getEmpId())).thenReturn(finance);
        assertThat(accessContext.requireEditor(finance).getRole()).isEqualTo(EmployeeRole.Finance);

        Employee marketer = employee(EmployeeRole.Marketer);
        when(employeeRepository.findByEmpId(marketer.getEmpId())).thenReturn(marketer);
        assertThatThrownBy(() -> accessContext.requireEditor(marketer))
                .hasMessageContaining("无权执行");
    }

    @Test
    void shouldMoveReceivableAndPayableStatusWithAmountsAndDueDate() {
        Receivable receivable = new Receivable();
        receivable.setAmount(new BigDecimal("1000"));
        receivable.setReceivedAmount(new BigDecimal("300"));
        accessContext.refreshReceivableStatus(receivable);
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.Partial);

        receivable.setReceivedAmount(new BigDecimal("1000"));
        accessContext.refreshReceivableStatus(receivable);
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.Received);

        Payable payable = new Payable();
        payable.setAmount(new BigDecimal("800"));
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setDueDate(LocalDate.now().minusDays(1));
        accessContext.refreshPayableStatus(payable);
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.Overdue);
    }

    @Test
    void shouldCopyProjectCustomerOrganizationToBusinessDocument() {
        Company company = new Company();
        company.setComId(10L);
        Store store = new Store();
        store.setStoreId(20L);
        Employee owner = employee(EmployeeRole.Marketer);
        Customer customer = new Customer();
        customer.setStore(store);
        customer.setOwner(owner);
        Project project = new Project();
        project.setProjectId(30L);
        project.setCompany(company);
        project.setCustomer(customer);

        Employee operator = employee(EmployeeRole.Finance);
        operator.setCompany(company);
        Receivable receivable = new Receivable();
        accessContext.applyOwnership(receivable, operator, project);

        assertThat(receivable.getCompany()).isSameAs(company);
        assertThat(receivable.getStore()).isSameAs(store);
        assertThat(receivable.getOwner()).isSameAs(owner);
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId((long) role.ordinal() + 1);
        employee.setName(role.getName());
        employee.setRole(role);
        return employee;
    }
}
