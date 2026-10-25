package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.project.service.reports.save.support.Save;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.decoboon.carryover.context.CarryoverLockContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** PD-X1：日报发布门禁必须在 Provider 入口生效，而非只依赖前端隐藏。 */
@ExtendWith(MockitoExtension.class)
class ProjectReportPublisherProviderTest {

    @Mock private TenantContext tenantContext;
    @Mock private DataScopeContext dataScopeContext;
    @Mock private ProjectRepository projectRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private ProjectReportRepository reportRepository;
    @Mock private ProjectStageRepository stageRepository;
    @Mock private ProjectTaskRepository taskRepository;
    @Mock private ProjectPermissionContext permissionContext;
    @Mock private CarryoverLockContext carryoverLockContext;

    private ProjectAccessContext accessContext;
    private Project project;

    @BeforeEach
    void setUp() {
        accessContext = new ProjectAccessContext(tenantContext, dataScopeContext, projectRepository, quoteRepository);
        Company company = company(22L);
        project = new Project();
        project.setProjectId(11L);
        project.setCompany(company);
        when(projectRepository.findOne(any(Specification.class))).thenReturn(Optional.of(project));
    }

    @Test
    void pmBossAndStoreManagerCanPublishThroughProvider() {
        for (EmployeeRole role : new EmployeeRole[]{EmployeeRole.Pm, EmployeeRole.Boss, EmployeeRole.StoreManager}) {
            Employee employee = employee(role);
            when(tenantContext.requireEmployee(employee)).thenReturn(employee);
            when(dataScopeContext.resolve(employee)).thenReturn(new DataScope(
                    DataScopeType.Company, 22L, null, null, employee.getEmpId()));
            when(reportRepository.save(any(ProjectReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Save provider = provider(employee);
            ProjectReport saved = provider.call(new ProjectReport());

            verify(reportRepository).save(saved);
        }
    }

    @Test
    void designerFinanceMarketerCannotPublishThroughProvider() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Designer, EmployeeRole.Finance, EmployeeRole.Marketer, EmployeeRole.TeamLeader}) {
            Employee employee = employee(role);
            when(tenantContext.requireEmployee(employee)).thenReturn(employee);
            when(dataScopeContext.resolve(employee)).thenReturn(new DataScope(
                    DataScopeType.Company, 22L, null, null, employee.getEmpId()));

            Save provider = provider(employee);

            assertThatThrownBy(() -> provider.call(new ProjectReport()))
                    .isInstanceOf(DefinedException.class)
                    .hasMessageContaining("只有项目经理、企业负责人或店长可以发布现场日报");
        }
        verify(reportRepository, never()).save(any(ProjectReport.class));
    }

    private Save provider(Employee employee) {
        Save provider = new Save(projectRepository, stageRepository, taskRepository, reportRepository,
                accessContext, permissionContext, carryoverLockContext);
        ReflectionTestUtils.setField(provider, "employee", employee);
        ReflectionTestUtils.setField(provider, "projectId", 11L);
        ReflectionTestUtils.setField(provider, "content", "现场施工进展");
        ReflectionTestUtils.setField(provider, "title", "现场日报");
        return provider;
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
