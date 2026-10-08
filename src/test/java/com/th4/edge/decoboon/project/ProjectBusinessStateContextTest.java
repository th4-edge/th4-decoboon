package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.context.ProjectBusinessStateContext;
import com.th4.edge.decoboon.project.model.Project;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 项目服务端状态视图测试。 */
class ProjectBusinessStateContextTest {

    private final ProjectBusinessStateContext context = new ProjectBusinessStateContext();

    @Test
    void preparingShouldReturnPreparationAction() {
        Project project = project(ProjectStatus.Preparing);
        context.enrich(project);
        assertThat(project.getBusinessStateView().getStatus()).isEqualTo("Preparing");
        assertThat(project.getBusinessStateView().getNextAction().getKey()).isEqualTo("project/start-preparation");
        assertThat(project.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Ongoing");
    }

    @Test
    void ongoingShouldReturnTaskAction() {
        Project project = project(ProjectStatus.Ongoing);
        context.enrich(project);
        assertThat(project.getBusinessStateView().getNextAction().getKey()).isEqualTo("project/advance-tasks");
        assertThat(project.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Accepting");
    }

    @Test
    void acceptingShouldReturnAcceptanceAction() {
        Project project = project(ProjectStatus.Accepting);
        context.enrich(project);
        assertThat(project.getBusinessStateView().getNextAction().getKey()).isEqualTo("project/handle-acceptance");
        assertThat(project.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Finished");
    }

    @Test
    void suspendedShouldReturnResumeAction() {
        Project project = project(ProjectStatus.Suspended);
        context.enrich(project);
        assertThat(project.getBusinessStateView().getNextAction().getKey()).isEqualTo("project/resume");
        assertThat(project.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Ongoing");
    }

    @Test
    void finishedShouldReturnSettlementAction() {
        Project project = project(ProjectStatus.Finished);
        context.enrich(project);
        assertThat(project.getBusinessStateView().getNextAction().getKey()).isEqualTo("settlement/open");
        assertThat(project.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Settled");
    }

    @Test
    void settledShouldReturnReadonlyArchiveAction() {
        Project project = project(ProjectStatus.Settled);
        context.enrich(project);
        assertThat(project.getBusinessStateView().getNextAction().getKey()).isEqualTo("project/archive");
        assertThat(project.getBusinessStateView().getNextAction().getEnabled()).isFalse();
    }

    @Test
    void missingProjectShouldBeIgnored() {
        context.enrich(null);
        context.enrich(new Project());
    }

    private Project project(ProjectStatus status) {
        Project project = new Project();
        project.setProjectId(1L);
        project.setStatus(status);
        project.setProgress(10);
        project.setCurrentStageName("水电");
        return project;
    }
}
