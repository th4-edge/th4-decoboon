package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.context.CustomerBatchContext;
import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerBatchPreview;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerBatchContextTest {

    @Test
    void parsesSpecialCharactersAndClassifiesValidDuplicateInvalid() {
        CustomerRepository repository = mock(CustomerRepository.class);
        CustomerContext customerContext = mock(CustomerContext.class);
        CustomerBatchContext context = new CustomerBatchContext(repository, customerContext);
        Company company = company(1L);
        Employee boss = employee(1L, EmployeeRole.Boss, company, store(1L), EmployeeStatus.Active);

        when(repository.findByCompany_ComId(1L)).thenReturn(List.of(customer("13800138000")));

        CustomerBatchPreview preview = context.preview(
                boss,
                "13800138000、13800138000, 13900139000/123；+86 13700137000|0086 13600136000"
        );

        assertThat(preview.getTotal()).isEqualTo(6);
        assertThat(preview.getValidCount()).isEqualTo(3);
        assertThat(preview.getDuplicateCount()).isEqualTo(2);
        assertThat(preview.getInvalidCount()).isEqualTo(1);
        assertThat(preview.getItems()).extracting(CustomerBatchPreview.Item::getStatus)
                .containsExactly("duplicate", "duplicate", "valid", "invalid", "valid", "valid");
    }

    @Test
    void savesOnlyValidRowsAndDeliversThemToSelectedOwner() {
        CustomerRepository repository = mock(CustomerRepository.class);
        CustomerContext customerContext = mock(CustomerContext.class);
        CustomerBatchContext context = new CustomerBatchContext(repository, customerContext);
        Company company = company(1L);
        Employee boss = employee(1L, EmployeeRole.Boss, company, store(1L), EmployeeStatus.Active);
        Employee marketer = employee(2L, EmployeeRole.Marketer, company, store(1L), EmployeeStatus.Active);

        when(repository.findByCompany_ComId(1L)).thenReturn(List.of());
        when(customerContext.resolveAssignableOwner(boss, 2L)).thenReturn(marketer);

        CustomerBatchPreview result = context.save(boss, "13800138000,13900139000", 2L);

        assertThat(result.getCreatedCount()).isEqualTo(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Customer>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        Customer first = captor.getValue().get(0);
        assertThat(first.getTelNo()).isEqualTo("13800138000");
        assertThat(first.getSource()).isEqualTo(CustomerSource.BatchPhone);
        assertThat(first.getStatus()).isEqualTo(CustStatus.New);
        assertThat(first.getName()).isEqualTo("待完善-138****8000");
        verify(customerContext).applyOwnership(first, marketer);
    }

    @Test
    void rejectsNonManagerBatchEntry() {
        CustomerBatchContext context = new CustomerBatchContext(mock(CustomerRepository.class), mock(CustomerContext.class));
        Employee marketer = employee(2L, EmployeeRole.Marketer, company(1L), store(1L), EmployeeStatus.Active);

        assertThatThrownBy(() -> context.preview(marketer, "13800138000"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("仅企业负责人、店长或财务");
    }

    @Test
    void financeCanUseBatchEntry() {
        CustomerRepository repository = mock(CustomerRepository.class);
        CustomerBatchContext context = new CustomerBatchContext(repository, mock(CustomerContext.class));
        Employee finance = employee(7L, EmployeeRole.Finance, company(1L), store(1L), EmployeeStatus.Active);
        when(repository.findByCompany_ComId(1L)).thenReturn(List.of());

        CustomerBatchPreview preview = context.preview(finance, "13800138000");

        assertThat(preview.getValidCount()).isEqualTo(1);
    }

    private static Customer customer(String phone) {
        Customer customer = new Customer();
        customer.setTelNo(phone);
        return customer;
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

    private static Employee employee(Long id, EmployeeRole role, Company company, Store store, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(role);
        employee.setCompany(company);
        employee.setStore(store);
        employee.setStatus(status);
        return employee;
    }
}
