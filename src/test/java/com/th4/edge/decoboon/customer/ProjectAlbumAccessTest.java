package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.service.project.album.page.support.Page;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 工地相册必须同时支持员工数据权限和业主项目授权，不能把员工会话当业主处理。 */
class ProjectAlbumAccessTest {

    @Test
    void employeeShouldReadAlbumThroughProjectDataScope() {
        CustomerAccessContext customerAccess = mock(CustomerAccessContext.class);
        ProjectAccessContext projectAccess = mock(ProjectAccessContext.class);
        ProjectReportRepository reportRepository = mock(ProjectReportRepository.class);
        AcceptanceRepository acceptanceRepository = mock(AcceptanceRepository.class);
        Page page = new Page(customerAccess, projectAccess, reportRepository, acceptanceRepository);

        Employee employee = new Employee();
        employee.setEmpId(9L);
        Project project = project(100L);
        ReflectionTestUtils.setField(page, "principal", employee);
        ReflectionTestUtils.setField(page, "projectId", 100L);
        when(projectAccess.requireReadableProject(100L, employee)).thenReturn(project);
        when(reportRepository
                .findByCompany_ComIdAndProject_ProjectIdAndVisibleToCustomerTrueOrderByCreateTime_DateTimeDesc(
                        any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(acceptanceRepository.findByProject_ProjectIdOrderByAcceptanceIdDesc(100L))
                .thenReturn(List.of());

        var result = page.call(new Project());

        assertThat(result.getTotalElements()).isZero();
        verify(projectAccess).requireReadableProject(100L, employee);
        verifyNoInteractions(customerAccess);
    }

    @Test
    void ownerShouldStillReadAlbumThroughExplicitProjectAccess() {
        CustomerAccessContext customerAccess = mock(CustomerAccessContext.class);
        ProjectAccessContext projectAccess = mock(ProjectAccessContext.class);
        ProjectReportRepository reportRepository = mock(ProjectReportRepository.class);
        AcceptanceRepository acceptanceRepository = mock(AcceptanceRepository.class);
        Page page = new Page(customerAccess, projectAccess, reportRepository, acceptanceRepository);

        Credentials owner = new Credentials();
        owner.setCreId(7L);
        Project project = project(100L);
        ReflectionTestUtils.setField(page, "principal", owner);
        ReflectionTestUtils.setField(page, "projectId", 100L);
        when(customerAccess.requireCredential(owner)).thenReturn(owner);
        when(customerAccess.requireProject(owner, 100L)).thenReturn(project);
        when(reportRepository
                .findByCompany_ComIdAndProject_ProjectIdAndVisibleToCustomerTrueOrderByCreateTime_DateTimeDesc(
                        any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(acceptanceRepository.findByProject_ProjectIdOrderByAcceptanceIdDesc(100L))
                .thenReturn(List.of());

        var result = page.call(new Project());

        assertThat(result.getTotalElements()).isZero();
        verify(customerAccess).requireProject(owner, 100L);
        verifyNoInteractions(projectAccess);
    }

    private Project project(Long projectId) {
        Company company = new Company();
        company.setComId(1L);
        Project project = new Project();
        project.setProjectId(projectId);
        project.setCompany(company);
        return project;
    }
}
