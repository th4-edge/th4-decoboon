package com.th4.edge.decoboon.carryover;

import com.th4.edge.decoboon.carryover.configure.CarryoverCostStatus;
import com.th4.edge.decoboon.carryover.context.CarryoverCostContext;
import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.decoboon.carryover.repository.CarryoverCostSnapshotRepository;
import com.th4.edge.decoboon.project.model.Project;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CarryoverCostContextTest {

    @Test
    void completeCarryoverCostCombinesWithPostCost() {
        CarryoverCostSnapshotRepository repository = mock(CarryoverCostSnapshotRepository.class);
        Project project = new Project();
        project.setProjectId(10L);
        CarryoverCostSnapshot snapshot = new CarryoverCostSnapshot();
        snapshot.setTotalCost(new BigDecimal("10000.00"));
        snapshot.setCostStatus(CarryoverCostStatus.Complete);
        when(repository.findByProject_ProjectId(10L)).thenReturn(Optional.of(snapshot));

        CarryoverCostContext.CombinedCost result = new CarryoverCostContext(repository)
                .combine(project, new BigDecimal("2000.00"), new BigDecimal("2500.00"), true);

        assertEquals(new BigDecimal("12000.00"), result.estimatedCost());
        assertEquals(new BigDecimal("12500.00"), result.actualCost());
        assertTrue(result.complete());
    }

    @Test
    void incompleteCarryoverCostKeepsProfitIncomplete() {
        CarryoverCostSnapshotRepository repository = mock(CarryoverCostSnapshotRepository.class);
        Project project = new Project();
        project.setProjectId(10L);
        CarryoverCostSnapshot snapshot = new CarryoverCostSnapshot();
        snapshot.setTotalCost(new BigDecimal("10000.00"));
        snapshot.setCostStatus(CarryoverCostStatus.TotalOnly);
        when(repository.findByProject_ProjectId(10L)).thenReturn(Optional.of(snapshot));

        CarryoverCostContext.CombinedCost result = new CarryoverCostContext(repository)
                .combine(project, BigDecimal.ZERO, BigDecimal.ZERO, true);

        assertFalse(result.complete());
        assertFalse(result.carryoverComplete());
    }
}
