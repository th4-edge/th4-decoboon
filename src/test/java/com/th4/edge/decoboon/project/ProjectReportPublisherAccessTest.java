package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** DEC-088 / BR-054：现场日报发布限项目经理、企业负责人、本店店长。 */
@ExtendWith(MockitoExtension.class)
class ProjectReportPublisherAccessTest {

    @Mock private TenantContext tenantContext;
    @Mock private DataScopeContext dataScopeContext;
    @Mock private ProjectRepository projectRepository;
    @Mock private QuoteRepository quoteRepository;

    private ProjectAccessContext context;
    @BeforeEach
    void setUp() {
        context = new ProjectAccessContext(tenantContext, dataScopeContext, projectRepository, quoteRepository);
    }

    @Test
    void pmBossAndStoreManagerCanPublish() {
        for (EmployeeRole role : new EmployeeRole[]{EmployeeRole.Pm, EmployeeRole.Boss, EmployeeRole.StoreManager}) {
            Employee employee = employee(role);
            when(tenantContext.requireEmployee(employee)).thenReturn(employee);

            context.requireReportPublisher(employee);
        }
    }

    @Test
    void designerFinanceMarketerAndTeamLeaderCannotPublish() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Designer, EmployeeRole.Finance, EmployeeRole.Marketer, EmployeeRole.TeamLeader}) {
            Employee employee = employee(role);
            when(tenantContext.requireEmployee(employee)).thenReturn(employee);

            assertThatThrownBy(() -> context.requireReportPublisher(employee))
                    .isInstanceOf(DefinedException.class)
                    .hasMessageContaining("只有项目经理、企业负责人或店长可以发布现场日报");
        }
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId((long) (role.ordinal() + 100));
        employee.setRole(role);
        employee.setCompany(company(22L));
        return employee;
    }

    private Company company(Long comId) {
        Company company = new Company();
        company.setComId(comId);
        return company;
    }
}
