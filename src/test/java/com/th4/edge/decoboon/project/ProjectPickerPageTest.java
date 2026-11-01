package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.service.projects.picker.page.support.Page;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 项目选择器必须使用轻量分页并限制单页大小。 */
class ProjectPickerPageTest {

    @Test
    void projectPickerUsesLightweightDatabasePage() {
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectStageRepository stages = mock(ProjectStageRepository.class);
        ProjectAccessContext access = mock(ProjectAccessContext.class);
        Employee boss = employee();
        when(access.requireEmployee(boss)).thenReturn(boss);
        when(access.visibleReadableProject(boss)).thenReturn((root, query, builder) -> builder.conjunction());
        when(projects.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Project>(List.of(), invocation.getArgument(1), 0));

        Page page = new Page(projects, stages, access);
        ReflectionTestUtils.setField(page, "employee", boss);
        ReflectionTestUtils.setField(page, "current", 1);
        ReflectionTestUtils.setField(page, "size", 200);

        assertThat(page.call(new Project()).getTotalElements()).isZero();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(projects).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }

    private static Employee employee() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(9L);
        employee.setCompany(company);
        return employee;
    }
}
