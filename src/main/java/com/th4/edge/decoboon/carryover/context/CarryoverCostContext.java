package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverCostStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.decoboon.carryover.repository.CarryoverCostSnapshotRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/** 接管前成本唯一读取口径。 */
@Note("接管前成本")
@Component
public class CarryoverCostContext {

    private final CarryoverCostSnapshotRepository snapshotRepository;

    public CarryoverCostContext(CarryoverCostSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public Optional<CarryoverCostSnapshot> snapshot(Project project) {
        if (project == null || project.getProjectId() == null) {
            return Optional.empty();
        }
        return snapshotRepository.findByProject_ProjectId(project.getProjectId());
    }

    public BigDecimal totalCost(Project project) {
        return snapshot(project).map(CarryoverCostSnapshot::getTotalCost).orElse(BigDecimal.ZERO);
    }

    public boolean complete(Project project) {
        return snapshot(project)
                .map(item -> item.getCostStatus() == CarryoverCostStatus.Complete)
                .orElse(false);
    }

    /** 接管前成本与接管后成本合并；任一段不完整时结果不完整。 */
    public CombinedCost combine(Project project, BigDecimal postEstimated, BigDecimal postActual,
                                boolean postComplete) {
        CarryoverCostSnapshot snapshot = snapshot(project).orElse(null);
        BigDecimal pre = snapshot == null ? BigDecimal.ZERO : value(snapshot.getTotalCost());
        boolean preComplete = snapshot != null && snapshot.getCostStatus() == CarryoverCostStatus.Complete;
        boolean complete = preComplete && postComplete;
        return new CombinedCost(
                value(postEstimated).add(pre),
                value(postActual).add(pre),
                complete ? 0 : 1,
                pre,
                preComplete);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record CombinedCost(BigDecimal estimatedCost, BigDecimal actualCost, int incompleteItemCount,
                               BigDecimal carryoverCost, boolean carryoverComplete) {
        public boolean complete() {
            return incompleteItemCount == 0;
        }
    }
}
