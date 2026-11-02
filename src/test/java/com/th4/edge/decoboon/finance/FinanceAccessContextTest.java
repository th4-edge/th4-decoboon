package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import com.th4.edge.decoboon.common.access.StoreAttributionTestSupport;
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
    private DataScopeContext dataScopeContext;
    private FinanceAccessContext accessContext;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        projectRepository = mock(ProjectRepository.class);
        ledgerRepository = mock(LedgerRepository.class);
        TenantContext tenantContext = new TenantContext(employeeRepository);
        dataScopeContext = mock(DataScopeContext.class);
        accessContext = StoreAttributionTestSupport.inject(new FinanceAccessContext(
                tenantContext,
                dataScopeContext,
                projectRepository,
                ledgerRepository
        ));
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
        customer.setCompany(company);
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


    @Test
    void shouldAllowHighRiskActionWithinResolvedCompanyOrStoreScope() {
        Employee manager = employee(EmployeeRole.StoreManager);
        Company company = new Company();
        company.setComId(10L);
        Store ownStore = new Store();
        ownStore.setStoreId(20L);
        manager.setCompany(company);
        manager.setStore(ownStore);
        when(employeeRepository.findByEmpId(manager.getEmpId())).thenReturn(manager);
        when(dataScopeContext.resolve(manager)).thenReturn(new DataScope(DataScopeType.Store, 10L, 20L, null, manager.getEmpId()));

        assertThat(accessContext.requireHighRiskEditor(manager).getRole()).isEqualTo(EmployeeRole.StoreManager);

        Ledger ownLedger = new Ledger();
        ownLedger.setStore(ownStore);
        accessContext.requireLedgerScope(manager, ownLedger);

        Store otherStore = new Store();
        otherStore.setStoreId(30L);
        Ledger otherLedger = new Ledger();
        otherLedger.setStore(otherStore);
        assertThatThrownBy(() -> accessContext.requireLedgerScope(manager, otherLedger))
                .hasMessageContaining("无权访问");
        Receipt ownReceipt = new Receipt();
        ownReceipt.setStore(ownStore);
        accessContext.requireReceiptScope(manager, ownReceipt);
        Receipt otherReceipt = new Receipt();
        otherReceipt.setStore(otherStore);
        assertThatThrownBy(() -> accessContext.requireReceiptScope(manager, otherReceipt))
                .hasMessageContaining("无权访问");

        Employee storeFinance = employee(EmployeeRole.Finance);
        storeFinance.setCompany(company);
        storeFinance.setStore(ownStore);
        when(employeeRepository.findByEmpId(storeFinance.getEmpId())).thenReturn(storeFinance);
        when(dataScopeContext.resolve(storeFinance))
                .thenReturn(new DataScope(DataScopeType.Store, 10L, 20L, null, storeFinance.getEmpId()));

        assertThat(accessContext.requireHighRiskEditor(storeFinance).getRole()).isEqualTo(EmployeeRole.Finance);
        accessContext.requireLedgerScope(storeFinance, ownLedger);
        accessContext.requireReceiptScope(storeFinance, ownReceipt);
        assertThatThrownBy(() -> accessContext.requireLedgerScope(storeFinance, otherLedger))
                .hasMessageContaining("无权访问");
        assertThatThrownBy(() -> accessContext.requireReceiptScope(storeFinance, otherReceipt))
                .hasMessageContaining("无权访问");

        Employee headFinance = employee(EmployeeRole.Finance);
        headFinance.setEmpId(998L);
        headFinance.setCompany(company);
        headFinance.setStore(ownStore);
        when(employeeRepository.findByEmpId(headFinance.getEmpId())).thenReturn(headFinance);
        when(dataScopeContext.resolve(headFinance))
                .thenReturn(new DataScope(DataScopeType.Company, 10L, null, null, headFinance.getEmpId()));

        assertThat(accessContext.requireHighRiskEditor(headFinance).getRole()).isEqualTo(EmployeeRole.Finance);
        accessContext.requireLedgerScope(headFinance, otherLedger);
        accessContext.requireReceiptScope(headFinance, otherReceipt);

        Employee marketer = employee(EmployeeRole.Marketer);
        when(employeeRepository.findByEmpId(marketer.getEmpId())).thenReturn(marketer);
        assertThatThrownBy(() -> accessContext.requireHighRiskEditor(marketer))
                .hasMessageContaining("无权执行");
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId((long) role.ordinal() + 1);
        employee.setName(role.getName());
        employee.setRole(role);
        return employee;
    }
}
