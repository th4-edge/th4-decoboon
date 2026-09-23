package com.th4.edge.decoboon.material;

import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.framework.exception.DefinedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 选材门禁（用户 2026-09-23 裁决：材料已进入采购后禁止再改选材，更换材料走增减项）。
 *
 * <p>与 {@code Confirm.promoteLinkedPlan} 的 D1 防护共用同一判定 {@code selectionLocked}，
 * 保证"禁止改选材"与"不回写选材快照"口径一致。</p>
 */
class MaterialSelectionGateTest {

    private MaterialSelectionContext context;

    @BeforeEach
    void setUp() {
        context = new MaterialSelectionContext(null, null, null, null, null, null, null);
    }

    @Test
    void plannedAndReadyToOrderShouldStayEditable() {
        assertThat(context.selectionLocked(plan(null))).isFalse();
        assertThat(context.selectionLocked(plan(MaterialPlanStatus.Planned))).isFalse();
        assertThat(context.selectionLocked(plan(MaterialPlanStatus.ReadyToOrder))).isFalse();
        assertThatCode(() -> context.requireSelectionEditable(List.of(plan(MaterialPlanStatus.ReadyToOrder))))
                .doesNotThrowAnyException();
    }

    @Test
    void everyPurchasingOrClosedStatusShouldBeLocked() {
        for (MaterialPlanStatus status : new MaterialPlanStatus[]{
                MaterialPlanStatus.Requested,
                MaterialPlanStatus.Approved,
                MaterialPlanStatus.PartiallyOrdered,
                MaterialPlanStatus.Ordered,
                MaterialPlanStatus.PartiallyArrived,
                MaterialPlanStatus.Arrived,
                MaterialPlanStatus.Closed,
                MaterialPlanStatus.Cancelled}) {
            assertThat(context.selectionLocked(plan(status)))
                    .as("状态 %s 应锁定选材", status)
                    .isTrue();
        }
    }

    @Test
    void lockedPlanShouldRejectSelectionChangeWithActionableMessage() {
        assertThatThrownBy(() -> context.requireSelectionEditable(List.of(plan(MaterialPlanStatus.Ordered))))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能再修改选材")
                .hasMessageContaining("发起增减项");
    }

    @Test
    void anyLockedPlanInSameContractLineShouldLockSelection() {
        List<MaterialPlan> plans = new ArrayList<>();
        plans.add(plan(MaterialPlanStatus.ReadyToOrder));
        MaterialPlan ordered = plan(MaterialPlanStatus.Ordered);
        ordered.setPlanId(2L);
        plans.add(ordered);
        assertThat(context.selectionLocked(plans)).isTrue();
        assertThatThrownBy(() -> context.requireSelectionEditable(plans))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("发起增减项");
        assertThat(context.selectionLocked(List.of(plan(MaterialPlanStatus.Planned)))).isFalse();
        assertThat(context.selectionLocked(List.<MaterialPlan>of())).isFalse();
    }

    private MaterialPlan plan(MaterialPlanStatus status) {
        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(1L);
        plan.setStatus(status);
        return plan;
    }
}
