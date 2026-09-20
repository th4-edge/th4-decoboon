package com.th4.edge.decoboon.property;

import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 客户交接后房屋责任归属回归测试。 */
class PropertyHandoffOwnershipTest {

    @Test
    void assignedDesignerMustBecomePropertyOwner() {
        PropertyRepository repository = mock(PropertyRepository.class);
        PropertyContext context = new PropertyContext(repository, mock(DataScopeContext.class));
        Property property = new Property();
        property.setStatus(PropertyStatus.PendingMeasure);
        Employee marketer = new Employee();
        marketer.setEmpId(10L);
        Employee designer = new Employee();
        designer.setEmpId(100L);
        property.setOwner(marketer);
        property.setSalesman(marketer);
        when(repository.save(property)).thenReturn(property);

        Property saved = context.assignDesigner(property, designer);

        assertThat(saved.getDesigner()).isSameAs(designer);
        assertThat(saved.getOwner()).isSameAs(marketer);
        assertThat(saved.getSalesman()).isSameAs(marketer);
        assertThat(saved.getStatus()).isEqualTo(PropertyStatus.Measuring);
    }

    @Test
    void customerAddressMustNotAutomaticallyCreateProperty() {
        PropertyRepository repository = mock(PropertyRepository.class);
        PropertyContext context = new PropertyContext(repository, mock(DataScopeContext.class));
        Customer customer = new Customer();
        customer.setCustId(1L);
        var company = new com.th4.edge.tenant.user.model.Company();
        company.setComId(2L);
        customer.setCompany(company);
        customer.setAddr("客户地址不能代替房屋档案");
        when(repository.findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(2L, 1L))
                .thenReturn(java.util.List.of());

        assertThatThrownBy(() -> context.ensureForCustomer(customer, new Employee()))
                .hasMessageContaining("请先建立房屋档案");

        verify(repository, never()).save(any(Property.class));
    }
}
