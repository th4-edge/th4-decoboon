package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerOwnerAssignmentTest {

    @Test
    void bossCanAssignCompanyMarketer() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        CustomerContext context = new CustomerContext(repository, mock(CustomerRepository.class), mock(DataScopeContext.class));
        Company company = company(1L);
        Store first = store(10L);
        Store second = store(20L);
        Employee boss = employee(1L, EmployeeRole.Boss, company, first);
        Employee marketer = employee(2L, EmployeeRole.Marketer, company, second);
        when(repository.findByEmpIdAndCompany_ComId(2L, 1L)).thenReturn(marketer);

        assertThat(context.resolveAssignableOwner(boss, 2L)).isSameAs(marketer);
    }

    @Test
    void storeManagerCanOnlyAssignSameStoreMarketer() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        CustomerContext context = new CustomerContext(repository, mock(CustomerRepository.class), mock(DataScopeContext.class));
        Company company = company(1L);
        Store first = store(10L);
        Store second = store(20L);
        Employee manager = employee(3L, EmployeeRole.StoreManager, company, first);
        Employee sameStore = employee(4L, EmployeeRole.Marketer, company, first);
        Employee otherStore = employee(5L, EmployeeRole.Marketer, company, second);
        when(repository.findByEmpIdAndCompany_ComId(4L, 1L)).thenReturn(sameStore);
        when(repository.findByEmpIdAndCompany_ComId(5L, 1L)).thenReturn(otherStore);

        assertThat(context.resolveAssignableOwner(manager, 4L)).isSameAs(sameStore);
        assertThatThrownBy(() -> context.resolveAssignableOwner(manager, 5L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("仅可转交给本门店人员");
    }

    @Test
    void financeCanBeAssignedAsCustomerSalesOwner() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        CustomerContext context = new CustomerContext(repository, mock(CustomerRepository.class), mock(DataScopeContext.class));
        Company company = company(1L);
        Store store = store(10L);
        Employee boss = employee(1L, EmployeeRole.Boss, company, store);
        Employee finance = employee(7L, EmployeeRole.Finance, company, store);
        when(repository.findByEmpIdAndCompany_ComId(7L, 1L)).thenReturn(finance);

        assertThat(context.resolveAssignableOwner(boss, 7L)).isSameAs(finance);
    }

    @Test
    void designerCanBeAssignedAsCustomerSalesOwner() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        CustomerContext context = new CustomerContext(repository, mock(CustomerRepository.class), mock(DataScopeContext.class));
        Company company = company(1L);
        Store store = store(10L);
        Employee boss = employee(1L, EmployeeRole.Boss, company, store);
        Employee designer = employee(6L, EmployeeRole.Designer, company, store);
        when(repository.findByEmpIdAndCompany_ComId(6L, 1L)).thenReturn(designer);

        assertThat(context.resolveAssignableOwner(boss, 6L)).isSameAs(designer);
    }

    @Test
    void designerCanOpenCustomerOwnedBySelfOrAssignedToSelf() {
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        DataScopeContext dataScopeContext = mock(DataScopeContext.class);
        CustomerContext context = new CustomerContext(employeeRepository, customerRepository, dataScopeContext);
        Company company = company(1L);
        Store store = store(10L);
        Employee designer = employee(6L, EmployeeRole.Designer, company, store);
        Customer ownerCustomer = customer(101L, company, designer, null);
        Customer assignedCustomer = customer(102L, company, employee(7L, EmployeeRole.Marketer, company, store), designer);
        when(dataScopeContext.requireEmployee(designer)).thenReturn(designer);
        when(dataScopeContext.resolve(designer)).thenReturn(new DataScope(DataScopeType.Self, 1L, 10L, null, 6L));
        when(customerRepository.findOne(org.mockito.ArgumentMatchers.<Specification<Customer>>any()))
                .thenReturn(Optional.of(ownerCustomer), Optional.empty());
        when(customerRepository.findByCustIdAndCompany_ComIdAndDesigner_EmpId(102L, 1L, 6L))
                .thenReturn(Optional.of(assignedCustomer));

        assertThat(context.requireVisible(101L, designer)).isSameAs(ownerCustomer);
        assertThat(context.requireVisible(102L, designer)).isSameAs(assignedCustomer);
    }

    private static Company company(Long id) {
        Company company = mock(Company.class);
        when(company.getComId()).thenReturn(id);
        return company;
    }

    private static Store store(Long id) {
        Store store = mock(Store.class);
        when(store.getStoreId()).thenReturn(id);
        return store;
    }

    private static Employee employee(Long id, EmployeeRole role, Company company, Store store) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(role);
        employee.setCompany(company);
        employee.setStore(store);
        employee.setStatus(EmployeeStatus.Active);
        return employee;
    }

    private static Customer customer(Long id, Company company, Employee owner, Employee designer) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setCompany(company);
        customer.setOwner(owner);
        customer.setDesigner(designer);
        return customer;
    }
}
