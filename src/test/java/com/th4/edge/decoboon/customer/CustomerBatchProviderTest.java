package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.context.CustomerBatchContext;
import com.th4.edge.decoboon.customer.model.CustomerBatch;
import com.th4.edge.decoboon.customer.model.CustomerBatchPreview;
import com.th4.edge.decoboon.customer.service.customerbatch.build.support.Build;
import com.th4.edge.decoboon.customer.service.customerbatch.save.support.Save;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerBatchProviderTest {

    @Test
    void buildLoadsFullTenantEmployeeBeforeParsing() {
        CustomerBatchContext context = mock(CustomerBatchContext.class);
        TenantContext tenantContext = mock(TenantContext.class);
        Employee principal = new Employee();
        principal.setEmpId(1L);
        Employee current = new Employee();
        current.setEmpId(1L);
        CustomerBatchPreview expected = new CustomerBatchPreview();
        when(tenantContext.requireEmployee(principal)).thenReturn(current);
        when(context.preview(current, "13800138000")).thenReturn(expected);
        Build provider = new Build(context, tenantContext);
        ReflectionTestUtils.setField(provider, "employee", principal);
        ReflectionTestUtils.setField(provider, "text", "13800138000");

        assertThat(provider.call(new CustomerBatch())).isSameAs(expected);
        verify(tenantContext).requireEmployee(principal);
    }

    @Test
    void saveLoadsFullTenantEmployeeBeforeCreatingCustomers() {
        CustomerBatchContext context = mock(CustomerBatchContext.class);
        TenantContext tenantContext = mock(TenantContext.class);
        Employee principal = new Employee();
        principal.setEmpId(1L);
        Employee current = new Employee();
        current.setEmpId(1L);
        CustomerBatchPreview expected = new CustomerBatchPreview();
        when(tenantContext.requireEmployee(principal)).thenReturn(current);
        when(context.save(current, "13800138000", 2L)).thenReturn(expected);
        Save provider = new Save(context, tenantContext);
        ReflectionTestUtils.setField(provider, "employee", principal);
        ReflectionTestUtils.setField(provider, "text", "13800138000");
        ReflectionTestUtils.setField(provider, "ownerId", 2L);

        assertThat(provider.call(new CustomerBatch())).isSameAs(expected);
        verify(tenantContext).requireEmployee(principal);
    }
}
