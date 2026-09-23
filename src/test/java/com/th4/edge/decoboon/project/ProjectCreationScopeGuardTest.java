package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.service.contract.save.support.Save;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 合同和项目创建必须同时通过角色能力与报价数据范围。 */
class ProjectCreationScopeGuardTest {

    @Test
    void marketerMustNotCreateContractEvenWhenQuoteIsPassedByFrontend() {
        ProjectAccessContext access = mock(ProjectAccessContext.class);
        var contractSave = mock(com.th4.edge.decoboon.project.context.ProjectContractContext.class);
        Employee marketer = employee(EmployeeRole.Marketer);
        Quote quote = new Quote();
        quote.setQuoteId(10L);
        quote.setStatus(QuoteStatus.Accepted);
        when(access.requireEmployee(marketer)).thenReturn(marketer);
        when(access.requireQuote(10L, marketer)).thenReturn(quote);
        Save save = new Save(access, contractSave);
        ReflectionTestUtils.setField(save, "employee", marketer);
        ReflectionTestUtils.setField(save, "quoteId", 10L);

        assertThatThrownBy(() -> save.call(new ProjectContract()))
                .hasMessageContaining("只有企业负责人可以处理合同签约");

        verify(access).requireQuote(10L, marketer);
    }

    @Test
    void storeManagerAndTeamLeaderMustNotPassContractSigningCapability() {
        for (EmployeeRole role : new EmployeeRole[]{EmployeeRole.StoreManager, EmployeeRole.TeamLeader}) {
            ProjectAccessContext access = mock(ProjectAccessContext.class);
            var contractContext = mock(com.th4.edge.decoboon.project.context.ProjectContractContext.class);
            Employee employee = employee(role);
            Quote quote = new Quote();
            quote.setQuoteId(10L);
            quote.setStatus(QuoteStatus.Accepted);
            when(access.requireEmployee(employee)).thenReturn(employee);
            when(access.requireQuote(10L, employee)).thenReturn(quote);
            Save save = new Save(access, contractContext);
            ReflectionTestUtils.setField(save, "employee", employee);
            ReflectionTestUtils.setField(save, "quoteId", 10L);

            assertThatThrownBy(() -> save.call(new ProjectContract()))
                    .hasMessageContaining("只有企业负责人可以处理合同签约");
            verify(access).requireQuote(10L, employee);
        }
    }

    @Test
    void crossCompanyContractAttemptMustReturnNotFoundForEveryRole() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Boss, EmployeeRole.Marketer, EmployeeRole.StoreManager,
                EmployeeRole.TeamLeader, EmployeeRole.Designer}) {
            ProjectAccessContext access = mock(ProjectAccessContext.class);
            var contractContext = mock(com.th4.edge.decoboon.project.context.ProjectContractContext.class);
            Employee employee = employee(role);
            when(access.requireEmployee(employee)).thenReturn(employee);
            when(access.requireQuote(10L, employee)).thenThrow(
                    new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
            Save save = new Save(access, contractContext);
            ReflectionTestUtils.setField(save, "employee", employee);
            ReflectionTestUtils.setField(save, "quoteId", 10L);

            assertThatThrownBy(() -> save.call(new ProjectContract()))
                    .hasMessageContaining("报价不存在或无权访问");
        }
    }

    @Test
    void onlyBossCanCreateContractAfterScopedAcceptedQuote() {
        for (EmployeeRole role : new EmployeeRole[]{EmployeeRole.Boss}) {
            ProjectAccessContext access = mock(ProjectAccessContext.class);
            var contractContext = mock(com.th4.edge.decoboon.project.context.ProjectContractContext.class);
            Employee employee = employee(role);
            Quote quote = new Quote();
            quote.setQuoteId(10L);
            quote.setStatus(QuoteStatus.Accepted);
            ProjectContract created = new ProjectContract();
            created.setContractId(20L);
            when(access.requireEmployee(employee)).thenReturn(employee);
            when(access.requireQuote(10L, employee)).thenReturn(quote);
            when(contractContext.createFromQuote(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(created);
            when(contractContext.enrich(created)).thenReturn(created);
            Save save = new Save(access, contractContext);
            ReflectionTestUtils.setField(save, "employee", employee);
            ReflectionTestUtils.setField(save, "quoteId", 10L);

            assertThat(save.call(new ProjectContract())).isSameAs(created);
        }
    }

    @Test
    void designerMustNotCreateContractEvenWhenScopedAcceptedQuote() {
        ProjectAccessContext access = mock(ProjectAccessContext.class);
        var contractContext = mock(com.th4.edge.decoboon.project.context.ProjectContractContext.class);
        Employee designer = employee(EmployeeRole.Designer);
        Quote quote = new Quote();
        quote.setQuoteId(10L);
        quote.setStatus(QuoteStatus.Accepted);
        when(access.requireEmployee(designer)).thenReturn(designer);
        when(access.requireQuote(10L, designer)).thenReturn(quote);
        Save save = new Save(access, contractContext);
        ReflectionTestUtils.setField(save, "employee", designer);
        ReflectionTestUtils.setField(save, "quoteId", 10L);

        assertThatThrownBy(() -> save.call(new ProjectContract()))
                .hasMessageContaining("只有企业负责人可以处理合同签约");
        verify(contractContext, never()).createFromQuote(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void signingDuringContractCreationMustProvideFinalSignedAmount() {
        ProjectAccessContext access = mock(ProjectAccessContext.class);
        var contractContext = mock(com.th4.edge.decoboon.project.context.ProjectContractContext.class);
        Employee employee = employee(EmployeeRole.Boss);
        Quote quote = new Quote();
        quote.setQuoteId(10L);
        quote.setStatus(QuoteStatus.Accepted);
        when(access.requireEmployee(employee)).thenReturn(employee);
        when(access.requireQuote(10L, employee)).thenReturn(quote);
        Save save = new Save(access, contractContext);
        ReflectionTestUtils.setField(save, "employee", employee);
        ReflectionTestUtils.setField(save, "quoteId", 10L);
        ReflectionTestUtils.setField(save, "signedAt", java.time.LocalDate.now());

        assertThatThrownBy(() -> save.call(new ProjectContract()))
                .hasMessageContaining("签约时必须填写最终签约价格");
        verify(contractContext, never()).createFromQuote(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void designerMustStillPassQuoteDataScopeOnContractCreation() {
        ProjectAccessContext access = mock(ProjectAccessContext.class);
        var contractSave = mock(com.th4.edge.decoboon.project.context.ProjectContractContext.class);
        Employee designer = employee(EmployeeRole.Designer);
        when(access.requireEmployee(designer)).thenReturn(designer);
        when(access.requireQuote(10L, designer)).thenThrow(
                new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        Save save = new Save(access, contractSave);
        ReflectionTestUtils.setField(save, "employee", designer);
        ReflectionTestUtils.setField(save, "quoteId", 10L);

        assertThatThrownBy(() -> save.call(new ProjectContract()))
                .hasMessageContaining("报价不存在或无权访问");
    }

    @Test
    void projectCreationMustRejectQuoteOutsideCurrentDataScope() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Marketer, EmployeeRole.TeamLeader, EmployeeRole.StoreManager}) {
            ProjectAccessContext access = mock(ProjectAccessContext.class);
            ProjectContext projectContext = mock(ProjectContext.class);
            Employee employee = employee(role);
            when(access.requireEmployee(employee)).thenReturn(employee);
            when(access.requireQuote(10L, employee)).thenThrow(
                    new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
            var save = new com.th4.edge.decoboon.project.service.projects.save.support.Save(
                    projectContext, mock(EmployeeRepository.class), access);
            ReflectionTestUtils.setField(save, "employee", employee);
            ReflectionTestUtils.setField(save, "quoteId", 10L);
            ReflectionTestUtils.setField(save, "name", "无权创建的项目");
            ReflectionTestUtils.setField(save, "siteAddr", "合成地址");

            assertThatThrownBy(() -> save.call(new Project()))
                    .hasMessageContaining("报价不存在或无权访问");

            verify(projectContext, never()).createFromQuote(any(Quote.class), any(Project.class));
        }
    }

    private Employee employee(EmployeeRole role) {
        var company = new com.th4.edge.tenant.user.model.Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(2L);
        employee.setCompany(company);
        employee.setRole(role);
        return employee;
    }
}
