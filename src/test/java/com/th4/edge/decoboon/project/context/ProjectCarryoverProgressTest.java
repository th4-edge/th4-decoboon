package com.th4.edge.decoboon.project.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 接管项目进度保护：剩余任务进度从接管基线继续增长，不能把进度算回零。 */
class ProjectCarryoverProgressTest {

    @Test
    void shouldKeepBaselineWhenNoRemainingTaskCompleted() {
        assertEquals(45, ProjectContext.blendProgress(45, 0));
    }

    @Test
    void shouldReachHundredWhenRemainingTasksCompleted() {
        assertEquals(100, ProjectContext.blendProgress(45, 100));
    }

    @Test
    void shouldAdvanceFromBaselineWithPartialRemainingProgress() {
        assertEquals(73, ProjectContext.blendProgress(45, 50));
    }
}
