package com.th4.edge.decoboon.common.model;

import com.th4.edge.decoboon.common.configure.FactStage;
import com.th4.edge.decoboon.common.configure.FactType;
import com.th4.edge.framework.core.annotation.Note;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 合同/项目事实节点的固定阶段、关联顺序和稳定排序契约。 */
@Note("业务事实排序契约")
public final class BusinessFactContract {

    public static final List<FactStage> PROJECT_STAGES = Arrays.stream(FactStage.values())
            .sorted(Comparator.comparingInt(FactStage::getOrder))
            .toList();

    public static final List<FactType> CONTRACT_UPSTREAM_TYPES = List.of(
            FactType.Measure, FactType.Design, FactType.Quote, FactType.Contract);

    public static final String NO_DESIGN_SOURCE = "无设计方案来源";
    public static final String SIGNING_QUOTE_BASELINE = "签约报价基线";
    public static final String CARRYOVER_SOURCE = "项目承接（接管基线）";

    private static final Comparator<BusinessFactNode> STABLE_ORDER = Comparator
            .comparing((BusinessFactNode node) -> node.getStage() == null ? Integer.MAX_VALUE : node.getStage().getOrder())
            .thenComparing(node -> node.getBusinessTime(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(node -> node.getRecordedAt(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(node -> node.getFactType() == null ? Integer.MAX_VALUE : node.getFactType().getOrder())
            .thenComparing(node -> node.getObjectId(), Comparator.nullsLast(Comparator.naturalOrder()));

    private BusinessFactContract() {
    }

    public static Comparator<BusinessFactNode> stableOrder() {
        return STABLE_ORDER;
    }

    public static boolean isProjectStageInOrder(List<FactStage> stages) {
        return PROJECT_STAGES.equals(stages);
    }

    public static boolean isContractUpstreamInOrder(List<FactType> types) {
        return CONTRACT_UPSTREAM_TYPES.equals(types);
    }

    /** 合同尚未建立项目时，上游节点允许 projectId 为空，但必须明确项目未建立。 */
    public static boolean isPreProjectFact(BusinessFactNode node) {
        return node != null && node.getProjectId() == null
                && node.getFactType() != null
                && (node.getFactType() == FactType.Customer
                || node.getFactType() == FactType.Property
                || node.getFactType() == FactType.Measure
                || node.getFactType() == FactType.Design
                || node.getFactType() == FactType.Quote
                || node.getFactType() == FactType.Contract)
                && node.getUncertainty() != null && !node.getUncertainty().isBlank();
    }

    public static void validateNode(BusinessFactNode node) {
        Objects.requireNonNull(node, "node");
        if (node.getStage() == null || node.getFactType() == null) {
            throw new IllegalArgumentException("事实节点必须包含 stage 和 factType");
        }
        if (node.getFactType().getStage() != node.getStage()) {
            throw new IllegalArgumentException("事实类型与阶段不匹配");
        }
        if (node.getObjectType() == null || node.getObjectType().isBlank()
                || node.getObjectId() == null || node.getVersion() == null
                || (node.getProjectId() == null && !isPreProjectFact(node))) {
            throw new IllegalArgumentException("事实节点必须绑定 objectType、objectId、projectId 和 version");
        }
        if (node.getCurrent() == null) {
            throw new IllegalArgumentException("事实节点必须明确 current 状态");
        }
        if (node.getNodeKey() == null || node.getNodeKey().isBlank()
                || node.getReturnPath() == null || node.getReturnPath().isBlank()
                || node.getReturnAnchor() == null || node.getReturnAnchor().isBlank()) {
            throw new IllegalArgumentException("事实节点必须包含 nodeKey、returnPath 和 returnAnchor");
        }
    }
}
