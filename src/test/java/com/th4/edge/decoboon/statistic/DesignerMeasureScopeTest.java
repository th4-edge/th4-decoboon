package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.statistic.context.DesignerMeasureTaskContext;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureQuery;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ANS-BIZ-20261011-002：BOSS 数据范围为 Company，量房队列按企业全量客户取数。 */
class DesignerMeasureScopeTest {

    @Test
    void bossQueueReadsWholeCompanyCustomers() {
        CustomerRepository customers = mock(CustomerRepository.class);
        var context = new DesignerMeasureTaskContext(customers, mock(PropertyRepository.class),
                mock(com.th4.edge.decoboon.property.repository.MeasureRepository.class),
                mock(com.th4.edge.decoboon.design.repository.RequirementRepository.class),
                mock(DesignRepository.class));
        when(customers.findByCompany_ComId(11L)).thenReturn(List.of());

        context.list(boss(), new DesignerMeasureQuery());

        verify(customers).findByCompany_ComId(11L);
        verify(customers, never()).findByCompany_ComIdAndOwner_EmpId(anyLong(), anyLong());
    }

    private Employee boss() {
        Company company = new Company();
        company.setComId(11L);
        Employee boss = new Employee();
        boss.setEmpId(33L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        return boss;
    }
}
