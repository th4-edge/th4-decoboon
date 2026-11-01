package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.context.ProjectManagerAccessContext;
import com.th4.edge.decoboon.project.service.manager.customers.page.support.Page;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectManagerCustomerPageEfficiencyTest {

    @Test
    void customerPageShouldUseDatabasePaginatedQueryInsteadOfLoadingManagedCustomers() {
        Company company = new Company();
        company.setComId(1L);
        Employee pm = new Employee();
        pm.setEmpId(9L);
        pm.setRole(EmployeeRole.Pm);
        pm.setCompany(company);

        ProjectManagerAccessContext access = mock(ProjectManagerAccessContext.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        when(access.requireManager(pm)).thenReturn(pm);
        when(customers.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Customer>(List.of(), invocation.getArgument(1), 0));

        Page page = new Page(access, customers);
        ReflectionTestUtils.setField(page, "employee", pm);
        ReflectionTestUtils.setField(page, "current", 1);
        ReflectionTestUtils.setField(page, "size", 20);

        assertThat(page.call(new Customer()).getTotalElements()).isZero();
        verify(customers).findAll(any(Specification.class), any(Pageable.class));
        verify(access, never()).managedCustomers(any());
    }
}
