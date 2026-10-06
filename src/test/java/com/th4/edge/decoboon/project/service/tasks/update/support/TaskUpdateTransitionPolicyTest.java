package com.th4.edge.decoboon.project.service.tasks.update.support;

import com.th4.edge.decoboon.project.configure.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 任务动作到状态和进度的无数据库策略测试。 */
class TaskUpdateTransitionPolicyTest {

    @Test
    void reopenShouldReturnToDoingAndResetProgress() {
        assertThat(Update.statusOf("reopen")).isEqualTo(TaskStatus.Doing);
        assertThat(Update.nextProgress(TaskStatus.Doing, 100, true)).isEqualTo(10);
    }

    @Test
    void startAndFinishShouldKeepContract() {
        assertThat(Update.statusOf("start")).isEqualTo(TaskStatus.Doing);
        assertThat(Update.nextProgress(TaskStatus.Doing, 30, false)).isEqualTo(30);
        assertThat(Update.statusOf("finish")).isEqualTo(TaskStatus.Accepting);
        assertThat(Update.nextProgress(TaskStatus.Accepting, 30, false)).isEqualTo(90);
    }

    @Test
    void doneAndTodoShouldHaveBoundaryProgress() {
        assertThat(Update.statusOf("done")).isEqualTo(TaskStatus.Done);
        assertThat(Update.nextProgress(TaskStatus.Done, 90, false)).isEqualTo(100);
        assertThat(Update.statusOf("todo")).isEqualTo(TaskStatus.Todo);
        assertThat(Update.nextProgress(TaskStatus.Todo, 90, false)).isZero();
    }
}
