package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerPortraitRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.repository.CustomerTagLinkRepository;
import com.th4.edge.decoboon.customer.service.cust.delete.support.Delete;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerDeleteProviderTest {

    @Test
    void bossDeletesLeadWithoutBusinessFacts() {
        Fixture fixture = fixture(EmployeeRole.Boss);
        when(fixture.customerRepository.findByCustIdAndCompany_ComId(9L, 1L))
                .thenReturn(Optional.of(fixture.customer));

        Customer deleted = fixture.provider.call(new Customer());

        assertThat(deleted).isSameAs(fixture.customer);
        verify(fixture.portraitRepository).deleteByCompany_ComIdAndCustomer_CustId(1L, 9L);
        verify(fixture.tagLinkRepository).deleteByCompany_ComIdAndCustomer_CustId(1L, 9L);
        verify(fixture.customerRepository).delete(fixture.customer);
    }

    @Test
    void blocksDeleteWhenBusinessFactsExist() {
        Fixture fixture = fixture(EmployeeRole.Boss);
        when(fixture.customerRepository.findByCustIdAndCompany_ComId(9L, 1L))
                .thenReturn(Optional.of(fixture.customer));
        when(fixture.followRepository.existsByCustomer_CustId(9L)).thenReturn(true);

        assertThatThrownBy(() -> fixture.provider.call(new Customer()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能删除");
        verify(fixture.customerRepository, never()).delete(fixture.customer);
    }

    @Test
    void onlyBossCanDelete() {
        Fixture fixture = fixture(EmployeeRole.StoreManager);

        assertThatThrownBy(() -> fixture.provider.call(new Customer()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("仅企业负责人");
    }

    private static Fixture fixture(EmployeeRole role) {
        TenantContext tenantContext = mock(TenantContext.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        CustomerFollowRepository followRepository = mock(CustomerFollowRepository.class);
        CustomerPortraitRepository portraitRepository = mock(CustomerPortraitRepository.class);
        CustomerTagLinkRepository tagLinkRepository = mock(CustomerTagLinkRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        DesignRepository designRepository = mock(DesignRepository.class);
        RequirementRepository requirementRepository = mock(RequirementRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ProjectContractRepository contractRepository = mock(ProjectContractRepository.class);
        Company company = mock(Company.class);
        when(company.getComId()).thenReturn(1L);
        Employee employee = new Employee();
        employee.setRole(role);
        employee.setCompany(company);
        when(tenantContext.requireEmployee(employee)).thenReturn(employee);
        Delete provider = new Delete(tenantContext, customerRepository, followRepository, portraitRepository,
                tagLinkRepository, propertyRepository, quoteRepository, designRepository,
                requirementRepository, projectRepository, contractRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(provider, "employee", employee);
        org.springframework.test.util.ReflectionTestUtils.setField(provider, "customerId", 9L);
        Customer customer = new Customer();
        customer.setCustId(9L);
        customer.setCompany(company);
        return new Fixture(provider, customerRepository, followRepository, portraitRepository,
                tagLinkRepository, customer);
    }

    private record Fixture(Delete provider,
                           CustomerRepository customerRepository,
                           CustomerFollowRepository followRepository,
                           CustomerPortraitRepository portraitRepository,
                           CustomerTagLinkRepository tagLinkRepository,
                           Customer customer) {
    }
}
