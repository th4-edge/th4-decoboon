package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 设计师录入客户时默认由本人承接设计。 */
class CustomerDesignerDefaultTest {

    @Test
    void designerCreatedCustomerDefaultsDesignerToSelf() {
        TenantContext tenant = mock(TenantContext.class);
        CustomerContext ownership = mock(CustomerContext.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        Employee designer = new Employee();
        designer.setEmpId(9L);
        designer.setRole(EmployeeRole.Designer);
        Customer request = new Customer();
        when(tenant.requireEmployee(designer)).thenReturn(designer);
        when(ownership.resolveOwner(designer, null, designer)).thenReturn(designer);
        when(customers.save(request)).thenReturn(request);
        com.th4.edge.decoboon.customer.service.cust.save.support.Save save =
                new com.th4.edge.decoboon.customer.service.cust.save.support.Save(
                        customers, ownership, tenant);
        ReflectionTestUtils.setField(save, "employee", designer);

        Customer saved = save.call(request);

        assertThat(saved.getDesigner()).isSameAs(designer);
    }
}
