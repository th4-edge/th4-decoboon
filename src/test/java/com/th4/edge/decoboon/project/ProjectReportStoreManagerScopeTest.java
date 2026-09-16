package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.service.reports.page.support.Page;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 店长首页动态必须按门店项目范围返回，而不是落空。 */
class ProjectReportStoreManagerScopeTest {

    @Test
    void storeManagerShouldReadReportsFromStoreProjects() {
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectReportRepository reports = mock(ProjectReportRepository.class);
        ProjectAccessContext access = mock(ProjectAccessContext.class);

        Company company = new Company();
        company.setComId(11L);
        Store store = new Store();
        store.setStoreId(22L);
        Employee manager = new Employee();
        manager.setEmpId(33L);
        manager.setRole(EmployeeRole.StoreManager);
        manager.setCompany(company);
        manager.setStore(store);

        Project project = new Project();
        project.setProjectId(44L);
        project.setCompany(company);
        Customer customer = new Customer();
        customer.setStore(store);
        project.setCustomer(customer);
        ProjectReport report = new ProjectReport();
        report.setProject(project);

        when(access.requireEmployee(manager)).thenReturn(manager);
        when(projects.findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(11L, 22L))
                .thenReturn(List.of(project));
        when(reports.findByCompany_ComIdOrderByCreateTime_DateTimeDesc(11L, PageRequest.of(0, 500)))
                .thenReturn(new PageImpl<>(List.of(report), PageRequest.of(0, 500), 1));

        Page provider = new Page(projects, reports, access);
        ReflectionTestUtils.setField(provider, "employee", manager);

        var result = provider.call(new ProjectReport());

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(report);
    }
}
