package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.project.configure.ProjectCostStage;
import com.th4.edge.decoboon.project.model.ProjectCostProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** DEC-092 / BR-058：项目成本三态投影契约。 */
class ProjectCostProjectionTest {

    private static final LocalDateTime AS_OF = LocalDateTime.of(2026, 10, 22, 20, 0);

    @Test
    void estimatedStageMustNeverExposeActualCost() {
        ProjectCostProjection projection = ProjectCostProjection.builder(ProjectCostStage.Estimated)
                .estimated(amount("100000"), amount("70000"), true, amount("30000"), amount("30.0"))
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, Map.of("quote", 3L))
                .build();

        assertThat(projection.costStage()).isEqualTo(ProjectCostStage.Estimated);
        assertThat(projection.getActualCost()).isNull();
        assertThat(projection.getTotalCost()).isEqualByComparingTo("70000");
        assertThat(projection.getCostComplete()).isTrue();
        assertThat(projection.getProfitAmount()).isEqualByComparingTo("30000");
        assertThat(projection.getGrossMargin()).isEqualByComparingTo("30.0");
        assertThat(projection.currentRevenue()).isEqualByComparingTo("100000");
        assertThat(projection.sourceSnapshotVersions()).containsEntry("quote", 3L);
    }

    @Test
    void dynamicStageMayUseProvisionalP2CostButKeepsFinalEmpty() {
        ProjectCostProjection projection = ProjectCostProjection.builder(ProjectCostStage.Dynamic)
                .estimated(amount("100000"), amount("70000"), true, amount("30000"), amount("30.0"))
                .dynamic(amount("100000"), amount("72000"), true, true, amount("28000"), amount("28.0"))
                .breakdown(amount("50000"), amount("12000"), amount("6000"), amount("2000"), amount("2000"), null, null)
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, Map.of("purchase", 5L))
                .build();

        assertThat(projection.dynamicCostProvisional()).isTrue();
        assertThat(projection.getActualCost()).isEqualByComparingTo("72000");
        assertThat(projection.getTotalCost()).isEqualByComparingTo("72000");
        assertThat(projection.getProfitAmount()).isEqualByComparingTo("28000");
        assertThat(projection.finalCost()).isNull();
        assertThat(projection.finalProfit()).isNull();
        assertThat(projection.finalCostComplete()).isFalse();
        assertThat(projection.materialCost()).isEqualByComparingTo("50000");
        assertThat(projection.workerLaborCost()).isEqualByComparingTo("12000");
        assertThat(projection.employeeSalaryCost()).isEqualByComparingTo("6000");
        assertThat(projection.employeeCommissionCost()).isEqualByComparingTo("2000");
        assertThat(projection.projectExpenseCost()).isEqualByComparingTo("2000");
    }

    @Test
    void dynamicStageWithoutP2MustRemainExplicitlyNonProvisional() {
        ProjectCostProjection projection = ProjectCostProjection.builder(ProjectCostStage.Dynamic)
                .dynamic(amount("100000"), amount("70000"), true, false, amount("30000"), amount("30.0"))
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, Map.of("settlement", 8L))
                .build();

        assertThat(projection.dynamicCostProvisional()).isFalse();
    }

    @Test
    void finalStageMustMapAliasesToFinalValues() {
        ProjectCostProjection projection = ProjectCostProjection.builder(ProjectCostStage.Final)
                .estimated(amount("100000"), amount("70000"), true, amount("30000"), amount("30.0"))
                .dynamic(amount("100000"), amount("72000"), true, false, amount("28000"), amount("28.0"))
                .finalized(amount("98000"), amount("71000"), true, amount("27000"), amount("27.55"))
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, Map.of("settlement", 8L))
                .build();

        assertThat(projection.getActualCost()).isEqualByComparingTo("71000");
        assertThat(projection.getTotalCost()).isEqualByComparingTo("71000");
        assertThat(projection.getCostComplete()).isTrue();
        assertThat(projection.getProfitAmount()).isEqualByComparingTo("27000");
        assertThat(projection.getGrossProfit()).isEqualByComparingTo("27000");
        assertThat(projection.getGrossMargin()).isEqualByComparingTo("27.55");
        assertThat(projection.dynamicCostProvisional()).isFalse();
    }

    @Test
    void incompleteStageMustRejectProfitAndMargin() {
        assertThatThrownBy(() -> ProjectCostProjection.builder(ProjectCostStage.Dynamic)
                .dynamic(amount("100000"), amount("50000"), false, true, amount("50000"), amount("50.0"))
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, Map.of("purchase", 5L))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dynamic profit and margin must be null");
    }

    @Test
    void completeStageMustHaveCost() {
        assertThatThrownBy(() -> ProjectCostProjection.builder(ProjectCostStage.Estimated)
                .estimated(amount("100000"), null, true, amount("30000"), amount("30.0"))
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, Map.of("quote", 3L))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimatedCost must not be null when complete");
    }

    @Test
    void formulaVersionMustUseSingleCurrentContractVersion() {
        assertThat(ProjectCostProjection.CURRENT_FORMULA_VERSION).isEqualTo("project-cost-projection-v1");

        for (String formulaVersion : new String[]{"", " ", "cost-v1", "caller-supplied-formula"}) {
            assertThatThrownBy(() -> ProjectCostProjection.builder(ProjectCostStage.Estimated)
                    .estimated(amount("100000"), amount("70000"), true, amount("30000"), amount("30.0"))
                    .metadata(formulaVersion, AS_OF, Map.of("quote", 3L))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("formulaVersion must equal");
        }

        ProjectCostProjection projection = ProjectCostProjection.builder(ProjectCostStage.Estimated)
                .estimated(amount("100000"), amount("70000"), true, amount("30000"), amount("30.0"))
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, Map.of("quote", 3L))
                .build();
        assertThat(projection.asOf()).isEqualTo(AS_OF);
    }

    @Test
    void sourceSnapshotVersionsMustBeNonEmptyCopiedAndImmutable() {
        Map<String, Long> mutableSource = new LinkedHashMap<>();
        mutableSource.put("quote", 3L);

        ProjectCostProjection projection = ProjectCostProjection.builder(ProjectCostStage.Estimated)
                .estimated(amount("100000"), amount("70000"), true, amount("30000"), amount("30.0"))
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, mutableSource)
                .build();

        mutableSource.put("quote", 99L);
        assertThat(projection.sourceSnapshotVersions()).containsExactlyEntriesOf(Map.of("quote", 3L));
        assertThatThrownBy(() -> projection.sourceSnapshotVersions().put("purchase", 5L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void sourceSnapshotVersionsMustRejectNullOrEmptyMap() {
        assertSourceVersionsRejected(null, "must not be empty");
        assertSourceVersionsRejected(Map.of(), "must not be empty");
    }

    @Test
    void sourceSnapshotVersionsMustRejectNullOrBlankKey() {
        Map<String, Long> nullKey = new HashMap<>();
        nullKey.put(null, 1L);
        assertSourceVersionsRejected(nullKey, "key must not be blank");
        assertSourceVersionsRejected(Map.of(" ", 1L), "key must not be blank");
    }

    @Test
    void sourceSnapshotVersionsMustRejectNullOrNonPositiveVersion() {
        Map<String, Long> nullVersion = new HashMap<>();
        nullVersion.put("quote", null);
        assertSourceVersionsRejected(nullVersion, "value must be positive");
        assertSourceVersionsRejected(Map.of("quote", 0L), "value must be positive");
        assertSourceVersionsRejected(Map.of("quote", -1L), "value must be positive");
    }

    @Test
    void nonPositiveRevenueMustNeverExposeMarginAcrossStages() {
        for (ProjectCostStage stage : ProjectCostStage.values()) {
            for (String revenue : new String[]{"0", "-100"}) {
                BigDecimal cost = amount("100");
                BigDecimal profit = amount(revenue).subtract(cost);
                assertThatThrownBy(() -> stageBuilder(stage, revenue, "100", true, profit, "10.0").build())
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining(" margin must be null when revenue <= 0");
            }
        }
    }

    @Test
    void nonPositiveRevenueMayKeepConsistentProfitButNotMargin() {
        for (ProjectCostStage stage : ProjectCostStage.values()) {
            ProjectCostProjection projection = stageBuilder(stage, "0", "100", true, amount("-100"), null).build();
            assertThat(projection.currentRevenue()).isEqualByComparingTo("0");
            assertThat(projection.currentProfit()).isEqualByComparingTo("-100");
            assertThat(projection.currentMargin()).isNull();
        }
    }

    @Test
    void completeStageMustRejectInconsistentProfitAndMarginAcrossStages() {
        for (ProjectCostStage stage : ProjectCostStage.values()) {
            assertThatThrownBy(() -> stageBuilder(
                    stage, "100000", "70000", true, amount("29999"), "30.0").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(" profit must equal revenue minus cost");

            assertThatThrownBy(() -> stageBuilder(
                    stage, "100000", "70000", true, amount("30000"), "31.0").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(" margin must equal profit / revenue * 100");

            assertThatThrownBy(() -> stageBuilder(
                    stage, "100000", "70000", true, amount("30000"), null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(" margin must not be null when revenue > 0");
        }
    }

    @ParameterizedTest
    @EnumSource(ProjectCostStage.class)
    void completeStageMustRejectProfitWhenRevenueIsUnknown(ProjectCostStage stage) {
        assertThatThrownBy(() -> stageBuilder(
                stage, null, "70000", true, amount("30000"), null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(" profit and margin must be null when revenue is null");
    }

    @Test
    void completePositiveRevenueMustAcceptRoundedConsistentMargin() {
        for (ProjectCostStage stage : ProjectCostStage.values()) {
            ProjectCostProjection projection = stageBuilder(
                    stage, "98000", "71000", true, amount("27000"), "27.55").build();
            assertThat(projection.currentMargin()).isEqualByComparingTo("27.55");
        }
    }

    @Test
    void estimatedAndFinalMustNotCarryProvisionalP2Marker() {
        assertThatThrownBy(() -> ProjectCostProjection.builder(ProjectCostStage.Estimated)
                .estimated(amount("100000"), amount("70000"), true, amount("30000"), amount("30.0"))
                .dynamic(amount("100000"), amount("72000"), true, true, amount("28000"), amount("28.0"))
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, Map.of("quote", 3L))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only allowed for dynamic stage");

        assertThatThrownBy(() -> ProjectCostProjection.builder(ProjectCostStage.Final)
                .dynamic(amount("100000"), amount("72000"), true, true, amount("28000"), amount("28.0"))
                .finalized(amount("98000"), amount("71000"), true, amount("27000"), amount("27.55"))
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, Map.of("settlement", 8L))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only allowed for dynamic stage");
    }

    private static void assertSourceVersionsRejected(Map<String, Long> versions, String message) {
        assertThatThrownBy(() -> ProjectCostProjection.builder(ProjectCostStage.Estimated)
                .estimated(amount("100000"), amount("70000"), true, amount("30000"), amount("30.0"))
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, versions)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(message);
    }

    private static ProjectCostProjection.Builder stageBuilder(ProjectCostStage stage,
                                                               String revenue,
                                                               String cost,
                                                               boolean complete,
                                                               BigDecimal profit,
                                                               String margin) {
        BigDecimal revenueValue = revenue == null ? null : amount(revenue);
        BigDecimal costValue = amount(cost);
        BigDecimal marginValue = margin == null ? null : amount(margin);
        ProjectCostProjection.Builder builder = ProjectCostProjection.builder(stage)
                .metadata(ProjectCostProjection.CURRENT_FORMULA_VERSION, AS_OF, Map.of("fixture", 1L));
        return switch (stage) {
            case Estimated -> builder.estimated(
                    revenueValue, costValue, complete, profit, marginValue);
            case Dynamic -> builder.dynamic(
                    revenueValue, costValue, complete, false, profit, marginValue);
            case Final -> builder.finalized(
                    revenueValue, costValue, complete, profit, marginValue);
        };
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
