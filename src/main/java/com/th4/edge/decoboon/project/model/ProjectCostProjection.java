package com.th4.edge.decoboon.project.model;

import com.th4.edge.decoboon.project.configure.ProjectCostStage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 项目成本与毛利的唯一三态只读投影。
 *
 * <p>DEC-092 / BR-058：预计只使用 P1；动态可使用已到货 P2 暂估并以 P3 优先；
 * 最终只使用 P3 与全部最终确认应计成本。收入未知时利润和毛利率必须为空；
 * 旧字段只作为带 {@code costStage} 的兼容别名，避免调用方继续使用无阶段
 * 语义的实际成本或利润。</p>
 */
public record ProjectCostProjection(
        ProjectCostStage costStage,
        BigDecimal estimatedRevenue,
        BigDecimal estimatedCost,
        boolean estimatedCostComplete,
        BigDecimal estimatedProfit,
        BigDecimal estimatedMargin,
        BigDecimal dynamicRevenue,
        BigDecimal dynamicCost,
        boolean dynamicCostComplete,
        boolean dynamicCostProvisional,
        BigDecimal dynamicProfit,
        BigDecimal dynamicMargin,
        BigDecimal finalRevenue,
        BigDecimal finalCost,
        boolean finalCostComplete,
        BigDecimal finalProfit,
        BigDecimal finalMargin,
        BigDecimal materialCost,
        BigDecimal workerLaborCost,
        BigDecimal employeeSalaryCost,
        BigDecimal employeeCommissionCost,
        BigDecimal projectExpenseCost,
        BigDecimal otherProjectAccruedCost,
        BigDecimal carryoverCost,
        String formulaVersion,
        LocalDateTime asOf,
        Map<String, Long> sourceSnapshotVersions) {

    /** 当前唯一允许的项目成本投影公式版本；生产方必须使用该版本。 */
    public static final String CURRENT_FORMULA_VERSION = "project-cost-projection-v1";

    private static final BigDecimal MARGIN_TOLERANCE = new BigDecimal("0.01");

    public ProjectCostProjection {
        Objects.requireNonNull(costStage, "costStage");
        if (!CURRENT_FORMULA_VERSION.equals(formulaVersion)) {
            throw new IllegalArgumentException(
                    "formulaVersion must equal " + CURRENT_FORMULA_VERSION);
        }
        Objects.requireNonNull(asOf, "asOf");
        requireStageContract("estimated", estimatedRevenue, estimatedCost, estimatedCostComplete,
                estimatedProfit, estimatedMargin);
        requireStageContract("dynamic", dynamicRevenue, dynamicCost, dynamicCostComplete,
                dynamicProfit, dynamicMargin);
        requireStageContract("final", finalRevenue, finalCost, finalCostComplete,
                finalProfit, finalMargin);
        if (dynamicCostProvisional && costStage != ProjectCostStage.Dynamic) {
            throw new IllegalArgumentException("dynamicCostProvisional is only allowed for dynamic stage");
        }
        sourceSnapshotVersions = requireSourceSnapshotVersions(sourceSnapshotVersions);
    }

    /** 兼容旧字段：预计阶段没有“实际成本”，动态/最终阶段映射到当前阶段成本。 */
    public BigDecimal getActualCost() {
        return costStage == ProjectCostStage.Estimated ? null : currentCost();
    }

    /** 兼容旧字段：必须结合 {@link #costStage()} 使用。 */
    public BigDecimal getTotalCost() {
        return currentCost();
    }

    /** 兼容旧字段：表示当前阶段的成本是否完整。 */
    public boolean getCostComplete() {
        return currentCostComplete();
    }

    /** 兼容旧字段：必须结合 {@link #costStage()} 使用。 */
    public BigDecimal getProfitAmount() {
        return currentProfit();
    }

    /** 兼容旧字段：必须结合 {@link #costStage()} 使用。 */
    public BigDecimal getGrossProfit() {
        return currentProfit();
    }

    /** 兼容旧字段：必须结合 {@link #costStage()} 使用。 */
    public BigDecimal getGrossMargin() {
        return currentMargin();
    }

    public BigDecimal currentRevenue() {
        return switch (costStage) {
            case Estimated -> estimatedRevenue;
            case Dynamic -> dynamicRevenue;
            case Final -> finalRevenue;
        };
    }

    public BigDecimal currentCost() {
        return switch (costStage) {
            case Estimated -> estimatedCost;
            case Dynamic -> dynamicCost;
            case Final -> finalCost;
        };
    }

    public boolean currentCostComplete() {
        return switch (costStage) {
            case Estimated -> estimatedCostComplete;
            case Dynamic -> dynamicCostComplete;
            case Final -> finalCostComplete;
        };
    }

    public BigDecimal currentProfit() {
        return switch (costStage) {
            case Estimated -> estimatedProfit;
            case Dynamic -> dynamicProfit;
            case Final -> finalProfit;
        };
    }

    public BigDecimal currentMargin() {
        return switch (costStage) {
            case Estimated -> estimatedMargin;
            case Dynamic -> dynamicMargin;
            case Final -> finalMargin;
        };
    }

    public static Builder builder(ProjectCostStage stage) {
        return new Builder(stage);
    }

    private static void requireStageContract(String stage,
                                             BigDecimal revenue,
                                             BigDecimal cost,
                                             boolean complete,
                                             BigDecimal profit,
                                             BigDecimal margin) {
        if (complete && cost == null) {
            throw new IllegalArgumentException(stage + "Cost must not be null when complete");
        }
        if (!complete && (profit != null || margin != null)) {
            throw new IllegalArgumentException(stage + " profit and margin must be null when incomplete");
        }
        if (!complete) {
            return;
        }
        if (revenue == null) {
            if (profit != null || margin != null) {
                throw new IllegalArgumentException(
                        stage + " profit and margin must be null when revenue is null");
            }
            return;
        }

        BigDecimal expectedProfit = revenue.subtract(cost);
        if (profit == null || profit.compareTo(expectedProfit) != 0) {
            throw new IllegalArgumentException(stage + " profit must equal revenue minus cost");
        }
        if (revenue.signum() <= 0) {
            if (margin != null) {
                throw new IllegalArgumentException(stage + " margin must be null when revenue <= 0");
            }
            return;
        }
        if (margin == null) {
            throw new IllegalArgumentException(stage + " margin must not be null when revenue > 0");
        }

        BigDecimal expectedMargin = expectedProfit.multiply(BigDecimal.valueOf(100))
                .divide(revenue, 6, RoundingMode.HALF_UP);
        if (margin.subtract(expectedMargin).abs().compareTo(MARGIN_TOLERANCE) > 0) {
            throw new IllegalArgumentException(stage + " margin must equal profit / revenue * 100");
        }
    }

    private static Map<String, Long> requireSourceSnapshotVersions(Map<String, Long> versions) {
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("sourceSnapshotVersions must not be empty");
        }
        for (Map.Entry<String, Long> entry : versions.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("sourceSnapshotVersions key must not be blank");
            }
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("sourceSnapshotVersions value must be positive");
            }
        }
        return Map.copyOf(versions);
    }

    /** 构造器只负责组装契约，不推断 P1/P2/P3 来源或利润公式。 */
    public static final class Builder {
        private final ProjectCostStage costStage;
        private BigDecimal estimatedRevenue;
        private BigDecimal estimatedCost;
        private boolean estimatedCostComplete;
        private BigDecimal estimatedProfit;
        private BigDecimal estimatedMargin;
        private BigDecimal dynamicRevenue;
        private BigDecimal dynamicCost;
        private boolean dynamicCostComplete;
        private boolean dynamicCostProvisional;
        private BigDecimal dynamicProfit;
        private BigDecimal dynamicMargin;
        private BigDecimal finalRevenue;
        private BigDecimal finalCost;
        private boolean finalCostComplete;
        private BigDecimal finalProfit;
        private BigDecimal finalMargin;
        private BigDecimal materialCost;
        private BigDecimal workerLaborCost;
        private BigDecimal employeeSalaryCost;
        private BigDecimal employeeCommissionCost;
        private BigDecimal projectExpenseCost;
        private BigDecimal otherProjectAccruedCost;
        private BigDecimal carryoverCost;
        private String formulaVersion;
        private LocalDateTime asOf;
        private Map<String, Long> sourceSnapshotVersions = Map.of();

        private Builder(ProjectCostStage costStage) {
            this.costStage = Objects.requireNonNull(costStage, "costStage");
        }

        public Builder estimated(BigDecimal revenue, BigDecimal cost, boolean complete,
                                 BigDecimal profit, BigDecimal margin) {
            this.estimatedRevenue = revenue;
            this.estimatedCost = cost;
            this.estimatedCostComplete = complete;
            this.estimatedProfit = profit;
            this.estimatedMargin = margin;
            return this;
        }

        public Builder dynamic(BigDecimal revenue, BigDecimal cost, boolean complete, boolean provisional,
                               BigDecimal profit, BigDecimal margin) {
            this.dynamicRevenue = revenue;
            this.dynamicCost = cost;
            this.dynamicCostComplete = complete;
            this.dynamicCostProvisional = provisional;
            this.dynamicProfit = profit;
            this.dynamicMargin = margin;
            return this;
        }

        public Builder finalized(BigDecimal revenue, BigDecimal cost, boolean complete,
                                 BigDecimal profit, BigDecimal margin) {
            this.finalRevenue = revenue;
            this.finalCost = cost;
            this.finalCostComplete = complete;
            this.finalProfit = profit;
            this.finalMargin = margin;
            return this;
        }

        public Builder breakdown(BigDecimal material, BigDecimal workerLabor, BigDecimal employeeSalary,
                                 BigDecimal employeeCommission, BigDecimal projectExpense,
                                 BigDecimal otherProjectAccrued, BigDecimal carryover) {
            this.materialCost = material;
            this.workerLaborCost = workerLabor;
            this.employeeSalaryCost = employeeSalary;
            this.employeeCommissionCost = employeeCommission;
            this.projectExpenseCost = projectExpense;
            this.otherProjectAccruedCost = otherProjectAccrued;
            this.carryoverCost = carryover;
            return this;
        }

        public Builder metadata(String formulaVersion, LocalDateTime asOf,
                                Map<String, Long> sourceSnapshotVersions) {
            this.formulaVersion = formulaVersion;
            this.asOf = asOf;
            this.sourceSnapshotVersions = sourceSnapshotVersions;
            return this;
        }

        public ProjectCostProjection build() {
            return new ProjectCostProjection(costStage, estimatedRevenue, estimatedCost,
                    estimatedCostComplete, estimatedProfit, estimatedMargin,
                    dynamicRevenue, dynamicCost, dynamicCostComplete, dynamicCostProvisional,
                    dynamicProfit, dynamicMargin,
                    finalRevenue, finalCost, finalCostComplete, finalProfit, finalMargin,
                    materialCost, workerLaborCost, employeeSalaryCost, employeeCommissionCost,
                    projectExpenseCost, otherProjectAccruedCost, carryoverCost,
                    formulaVersion, asOf, sourceSnapshotVersions);
        }
    }
}
