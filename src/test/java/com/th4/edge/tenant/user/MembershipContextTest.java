package com.th4.edge.tenant.user;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.common.context.StoreAttributionContext;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.repository.CompanyJoinLimitRepository;
import com.th4.edge.tenant.user.repository.CompanyMemberLogRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.InviteRepository;
import com.th4.edge.tenant.user.repository.TeamRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MembershipContextTest {

    @Test
    void shouldRejectLastBossResignation() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        MembershipContext context = new MembershipContext(
                mock(UserIdentityContext.class), employees, mock(InviteRepository.class),
                mock(com.th4.edge.tenant.user.repository.StoreRepository.class), mock(CompanyMemberLogRepository.class),
                mock(CompanyJoinLimitRepository.class), mock(SubscriptionContext.class),
                mock(StoreAttributionContext.class), mock(ModelIdentityContext.class), mock(CustomerRepository.class),
                mock(PropertyRepository.class), mock(QuoteRepository.class),
                mock(ProjectRepository.class), mock(ProjectTaskRepository.class),
                mock(ReceivableRepository.class));
        Company company = new Company();
        company.setComId(1L);
        Credentials user = new Credentials();
        user.setCreId(2L);
        user.setPhone("13800000000");
        Employee boss = new Employee();
        boss.setEmpId(3L);
        boss.setName("唯一老板");
        boss.setRole(EmployeeRole.Boss);
        boss.setStatus(EmployeeStatus.Active);
        boss.setCompany(company);
        user.setEmployee(boss);
        when(employees.countByCompany_ComIdAndRoleAndStatus(1L, EmployeeRole.Boss, EmployeeStatus.Active))
                .thenReturn(1L);

        assertThatThrownBy(() -> context.resign(boss, "主动退出", false))
                .hasMessageContaining("唯一负责人不能退出");
    }
}
