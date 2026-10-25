package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerLossReason;
import com.th4.edge.decoboon.customer.configure.CustomerLossScope;
import com.th4.edge.decoboon.customer.configure.CustomerLossStage;
import com.th4.edge.decoboon.customer.configure.CustomerStatusChangeType;
import com.th4.edge.decoboon.customer.context.CustomerLossContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerLoss;
import com.th4.edge.decoboon.customer.model.CustomerStatusLog;
import com.th4.edge.decoboon.customer.repository.CustomerLossRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.repository.CustomerStatusLogRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerLossContextTest {

    private CustomerRepository customerRepository;
    private CustomerLossRepository lossRepository;
    private CustomerStatusLogRepository logRepository;
    private PropertyRepository propertyRepository;
    private ProjectContractRepository contractRepository;
    private CustomerLossContext context;
    private Employee marketer;
    private Company company;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        lossRepository = mock(CustomerLossRepository.class);
        logRepository = mock(CustomerStatusLogRepository.class);
        propertyRepository = mock(PropertyRepository.class);
        contractRepository = mock(ProjectContractRepository.class);
        context = new CustomerLossContext(customerRepository, lossRepository, logRepository,
                propertyRepository, contractRepository);

        company = new Company();
        company.setComId(10L);
        marketer = new Employee();
        marketer.setEmpId(20L);
        marketer.setRole(EmployeeRole.Marketer);
        marketer.setCompany(company);

        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lossRepository.save(any(CustomerLoss.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(logRepository.save(any(CustomerStatusLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractRepository.existsByCompany_ComIdAndCustomer_CustIdAndStatusNotIn(
                anyLong(), anyLong(), any(Collection.class))).thenReturn(false);
        when(contractRepository.existsByCompany_ComIdAndProperty_PropertyIdAndStatusNotIn(
                anyLong(), anyLong(), any(Collection.class))).thenReturn(false);
    }

    @Test
    void shouldMarkWholeCustomerLostAndWriteStatusLog() {
        Customer customer = customer(CustStatus.Following);
        customer.setNextFollowAt(LocalDateTime.now().plusDays(1));

        CustomerLoss loss = context.markLost(customer, marketer, CustomerLossScope.Customer,
                CustomerLossStage.Design, CustomerLossReason.ChoseOtherCompany,
                "客户已确定其他公司", "XX装饰", null, CustStatus.Following, "loss-req-1");

        assertThat(loss.getScope()).isEqualTo(CustomerLossScope.Customer);
        assertThat(loss.getStage()).isEqualTo(CustomerLossStage.Design);
        assertThat(customer.getStatus()).isEqualTo(CustStatus.Lost);
        assertThat(customer.getNextFollowAt()).isNull();
        ArgumentCaptor<CustomerStatusLog> captor = ArgumentCaptor.forClass(CustomerStatusLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getChangeType()).isEqualTo(CustomerStatusChangeType.Lost);
        assertThat(captor.getValue().getToStatus()).isEqualTo(CustStatus.Lost);
    }

    @Test
    void propertyLossKeepsCustomerActiveWhenAnotherPropertyRemains() {
        Customer customer = customer(CustStatus.Following);
        Property lostProperty = property(customer, 101L, PropertyStatus.Designing);
        Property activeProperty = property(customer, 102L, PropertyStatus.Designing);
        when(propertyRepository.findByPropertyIdAndCompany_ComId(101L, 10L)).thenReturn(Optional.of(lostProperty));
        when(propertyRepository.findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(10L, 1L))
                .thenReturn(List.of(lostProperty, activeProperty));
        when(lossRepository.findActivePropertyIds(10L, 1L)).thenReturn(List.of(101L));

        context.markLost(customer, marketer, CustomerLossScope.Property, CustomerLossStage.Design,
                CustomerLossReason.ChoseOtherCompany, null, null, 101L, CustStatus.Following, "loss-req-2");

        assertThat(customer.getStatus()).isEqualTo(CustStatus.Following);
        verify(customerRepository, never()).save(customer);
        verify(logRepository, never()).save(any(CustomerStatusLog.class));
    }

    @Test
    void propertyLossMarksCustomerLostWhenAllOpenPropertiesAreLost() {
        Customer customer = customer(CustStatus.Quoted);
        Property property = property(customer, 101L, PropertyStatus.Quoted);
        when(propertyRepository.findByPropertyIdAndCompany_ComId(101L, 10L)).thenReturn(Optional.of(property));
        when(propertyRepository.findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(10L, 1L))
                .thenReturn(List.of(property));
        when(lossRepository.findActivePropertyIds(10L, 1L)).thenReturn(List.of(101L));

        context.markLost(customer, marketer, CustomerLossScope.Property, CustomerLossStage.Negotiation,
                CustomerLossReason.PriceBudgetMismatch, null, null, 101L, CustStatus.Quoted, "loss-req-3");

        assertThat(customer.getStatus()).isEqualTo(CustStatus.Lost);
        verify(customerRepository).save(customer);
    }

    @Test
    void shouldRejectMissingScope() {
        assertThatThrownBy(() -> context.markLost(customer(CustStatus.Following), marketer,
                null, CustomerLossStage.FollowUp, CustomerLossReason.ChoseOtherCompany,
                null, null, null, CustStatus.Following, "loss-req-scope"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("请选择流失范围");
    }

    @Test
    void shouldRejectOtherReasonWithoutDetail() {
        assertThatThrownBy(() -> context.markLost(customer(CustStatus.Following), marketer,
                CustomerLossScope.Customer, CustomerLossStage.FollowUp,
                CustomerLossReason.Other, " ", null, null, CustStatus.Following, "loss-req-4"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("必须填写");
    }

    @Test
    void shouldRejectSignedCustomer() {
        assertThatThrownBy(() -> context.markLost(customer(CustStatus.Signed), marketer,
                CustomerLossScope.Customer, CustomerLossStage.Signing,
                CustomerLossReason.ChoseOtherCompany, null, null, null, CustStatus.Signed, "loss-req-5"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("已签约");
    }

    @Test
    void financeCanOperateCustomerLoss() {
        Employee finance = new Employee();
        finance.setEmpId(30L);
        finance.setRole(EmployeeRole.Finance);
        finance.setCompany(company);

        assertThatCode(() -> context.requireWritableRole(finance)).doesNotThrowAnyException();
    }

    @Test
    void restoreShouldReturnCustomerToPreviousActiveStatus() {
        Customer customer = customer(CustStatus.Lost);
        CustomerLoss loss = new CustomerLoss();
        loss.setCompany(company);
        loss.setCustomer(customer);
        loss.setScope(CustomerLossScope.Customer);
        loss.setStage(CustomerLossStage.Design);
        loss.setFromStatus(CustStatus.PendingMeasure);
        loss.setLostAt(LocalDateTime.now());
        when(lossRepository.countByCompany_ComIdAndCustomer_CustIdAndRestoredAtIsNull(10L, 1L)).thenReturn(1L);

        CustomerLoss restored = context.restore(loss, customer, marketer, null,
                LocalDateTime.now().plusDays(2), "客户重新联系", "restore-req-1");

        assertThat(restored.getRestoredAt()).isNotNull();
        assertThat(customer.getStatus()).isEqualTo(CustStatus.PendingMeasure);
        assertThat(customer.getNextFollowAt()).isNotNull();
        ArgumentCaptor<CustomerStatusLog> captor = ArgumentCaptor.forClass(CustomerStatusLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getChangeType()).isEqualTo(CustomerStatusChangeType.RestoreLost);
    }

    private Customer customer(CustStatus status) {
        Customer customer = new Customer();
        customer.setCustId(1L);
        customer.setCompany(company);
        customer.setName("测试客户");
        customer.setOwner(marketer);
        customer.setStatus(status);
        return customer;
    }

    private Property property(Customer customer, Long propertyId, PropertyStatus status) {
        Property property = new Property();
        property.setPropertyId(propertyId);
        property.setCompany(company);
        property.setCustomer(customer);
        property.setStatus(status);
        property.setPropertyNo("P-" + propertyId);
        return property;
    }
}
