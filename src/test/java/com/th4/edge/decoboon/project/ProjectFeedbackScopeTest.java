package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.customer.configure.CustomerFeedbackKind;
import com.th4.edge.decoboon.customer.configure.CustomerFeedbackStatus;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业主意见投诉的员工侧可见范围必须与其它项目子对象一致（数据范围 + 项目参与人）。
 *
 * <p>历史实现只用 DataScope 过滤；业主投诉的 owner 是项目经理，导致项目设计师（Self 数据范围）
 * 在自己的项目上「一条都看不到、也答复不了」，Web/App 的「客户意见投诉」对设计师永久为空。</p>
 */
class ProjectFeedbackScopeTest {

    private Employee designer() {
        Employee designer = new Employee();
        designer.setEmpId(105L);
        designer.setRole(EmployeeRole.Designer);
        return designer;
    }

    @Test
    void pageShouldScopeByProjectParticipantsInsteadOfRawOwnerScope() {
        ProjectAccessContext access = mock(ProjectAccessContext.class);
        CustomerFeedbackRepository repository = mock(CustomerFeedbackRepository.class);
        Employee designer = designer();
        CustomerFeedback complaint = new CustomerFeedback();
        complaint.setKind(CustomerFeedbackKind.Complaint);
        complaint.setStatus(CustomerFeedbackStatus.Submitted);
        @SuppressWarnings("unchecked")
        Specification<CustomerFeedback> visible = mock(Specification.class);

        when(access.requireEmployee(designer)).thenReturn(designer);
        when(access.visibleChild(designer, CustomerFeedback.class, "project")).thenReturn(visible);
        when(repository.findAll(visible)).thenReturn(List.of(complaint));

        com.th4.edge.decoboon.project.service.feedback.page.support.Page provider =
                new com.th4.edge.decoboon.project.service.feedback.page.support.Page(access, repository);
        ReflectionTestUtils.setField(provider, "employee", designer);

        List<CustomerFeedback> result = provider.call(new CustomerFeedback());

        assertThat(result).containsExactly(complaint);
        verify(access).visibleChild(designer, CustomerFeedback.class, "project");
    }

    @Test
    void updateShouldLoadTheSameVisibleScopeSoDesignerCanRespond() {
        ProjectAccessContext access = mock(ProjectAccessContext.class);
        CustomerFeedbackRepository repository = mock(CustomerFeedbackRepository.class);
        Employee designer = designer();
        CustomerFeedback complaint = new CustomerFeedback();
        complaint.setStatus(CustomerFeedbackStatus.Submitted);
        when(access.requireEmployee(designer)).thenReturn(designer);
        when(access.visibleChild(eq(designer), eq(CustomerFeedback.class), eq("project")))
                .thenAnswer(invocation -> Specification.where(null));
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(complaint));
        when(repository.save(any(CustomerFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.th4.edge.decoboon.project.service.feedback.update.support.Update provider =
                new com.th4.edge.decoboon.project.service.feedback.update.support.Update(access, repository);
        ReflectionTestUtils.setField(provider, "employee", designer);
        ReflectionTestUtils.setField(provider, "feedbackId", 7L);
        ReflectionTestUtils.setField(provider, "status", CustomerFeedbackStatus.Resolved);
        ReflectionTestUtils.setField(provider, "response", " 已安排返工 ");

        CustomerFeedback result = provider.call(new CustomerFeedback());

        assertThat(result.getStatus()).isEqualTo(CustomerFeedbackStatus.Resolved);
        assertThat(result.getResponse()).isEqualTo("已安排返工");
        assertThat(result.getHandler()).isEqualTo(designer);
        assertThat(result.getRespondedAt()).isNotNull();
        verify(access).visibleChild(designer, CustomerFeedback.class, "project");
    }
}
