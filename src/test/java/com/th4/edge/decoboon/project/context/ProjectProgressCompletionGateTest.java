package com.th4.edge.decoboon.project.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectProgressCompletionGateTest {

    @Test
    void shouldNotReportOneHundredWhileAnyStageRemainsUnfinished() {
        assertThat(ProjectContext.capProjectProgress(false, 100)).isEqualTo(99);
        assertThat(ProjectContext.capProjectProgress(false, 120)).isEqualTo(99);
    }

    @Test
    void shouldKeepCompletedProjectAtOneHundred() {
        assertThat(ProjectContext.capProjectProgress(true, 100)).isEqualTo(100);
    }

    @Test
    void shouldPreserveOrdinaryProgressWithinBounds() {
        assertThat(ProjectContext.capProjectProgress(false, 42)).isEqualTo(42);
        assertThat(ProjectContext.capProjectProgress(true, -5)).isZero();
    }
}
