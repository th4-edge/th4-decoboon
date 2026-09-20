package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.service.cust.measure.support.Measure;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 客户约定量房状态和房屋门禁。 */
class CustomerMeasureStateGateTest {

    @Test
    void newCustomerMustNotScheduleMeasureBeforeFirstFollowUp() {
        CustomerRepository customers = mock(CustomerRepository.class);
        PropertyContext properties = mock(PropertyContext.class);
        Measure provider = provider(customers, properties);
        Customer customer = customer(CustStatus.New);
        when(customers.findOne(org.mockito.ArgumentMatchers.<Specification<Customer>>any()))
                .thenReturn(java.util.Optional.of(customer));

        assertThatThrownBy(() -> provider.call(new Customer()))
                .hasMessageContaining("请先完成首次跟进");

        verify(properties, never()).resolve(any(), any(), any());
    }

    @Test
    void followingCustomerWithoutPropertyMustFailBeforeScheduling() {
        CustomerRepository customers = mock(CustomerRepository.class);
        PropertyContext properties = mock(PropertyContext.class);
        Measure provider = provider(customers, properties);
        Customer customer = customer(CustStatus.Following);
        when(customers.findOne(org.mockito.ArgumentMatchers.<Specification<Customer>>any()))
                .thenReturn(java.util.Optional.of(customer));
        when(properties.resolve(any(), any(), any())).thenThrow(
                new DefinedException(new DefaultErrorCode("400", "请先建立房屋档案")));

        assertThatThrownBy(() -> provider.call(new Customer()))
                .hasMessageContaining("请先建立房屋档案");

        verify(customers, never()).save(any(Customer.class));
    }

    @Test
    void measuredPropertyMustNotAcceptAnotherMeasure() {
        CustomerRepository customers = mock(CustomerRepository.class);
        PropertyContext properties = mock(PropertyContext.class);
        Measure provider = provider(customers, properties);
        Customer customer = customer(CustStatus.Following);
        Property property = new Property();
        property.setPropertyId(4L);
        property.setStatus(PropertyStatus.Measured);
        when(customers.findOne(org.mockito.ArgumentMatchers.<Specification<Customer>>any()))
                .thenReturn(java.util.Optional.of(customer));
        when(properties.resolve(any(), any(), any())).thenReturn(property);

        assertThatThrownBy(() -> provider.call(new Customer()))
                .hasMessageContaining("不能重复预约");

        verify(customers, never()).save(any(Customer.class));
    }

    private Measure provider(CustomerRepository customers, PropertyContext properties) {
        Employee employee = new Employee();
        employee.setEmpId(1L);
        Company company = new Company();
        company.setComId(2L);
        employee.setCompany(company);
        DataScopeContext scopes = mock(DataScopeContext.class);
        when(scopes.requireEmployee(employee)).thenReturn(employee);
        when(scopes.resolve(employee)).thenReturn(new DataScope(
                DataScopeType.Company, 2L, null, null, 1L));
        Measure provider = new Measure(customers, scopes, mock(NoticeContext.class),
                properties, mock(MeasureRepository.class));
        ReflectionTestUtils.setField(provider, "employee", employee);
        ReflectionTestUtils.setField(provider, "customerId", 3L);
        ReflectionTestUtils.setField(provider, "propertyId", 4L);
        ReflectionTestUtils.setField(provider, "measureAt", LocalDateTime.now().plusDays(1));
        return provider;
    }

    private Customer customer(CustStatus status) {
        Company company = new Company();
        company.setComId(2L);
        Customer customer = new Customer();
        customer.setCustId(3L);
        customer.setCompany(company);
        customer.setStatus(status);
        return customer;
    }
}
