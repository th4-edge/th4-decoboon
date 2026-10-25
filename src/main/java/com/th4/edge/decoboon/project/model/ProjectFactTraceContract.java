package com.th4.edge.decoboon.project.model;

import com.th4.edge.decoboon.common.configure.FactStage;
import com.th4.edge.decoboon.common.configure.FactVisibility;
import com.th4.edge.decoboon.common.model.BusinessFactContract;
import com.th4.edge.decoboon.common.model.BusinessFactNode;
import com.th4.edge.framework.core.annotation.Note;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

/** 项目事实追踪的排序、分组、业主裁剪和来源绑定契约。 */
@Note("项目事实追踪契约")
public final class ProjectFactTraceContract {

    private ProjectFactTraceContract() {
    }

    public static List<BusinessFactNode> ordered(List<BusinessFactNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return List.of();
        return nodes.stream()
                .filter(Objects::nonNull)
                .sorted(BusinessFactContract.stableOrder())
                .toList();
    }

    public static List<BusinessFactNode> ownerVisible(List<BusinessFactNode> nodes) {
        return ordered(nodes).stream()
                .filter(node -> node.getVisibility() == FactVisibility.OwnerVisible)
                .toList();
    }

    public static Map<FactStage, List<BusinessFactNode>> phaseGroups(List<BusinessFactNode> nodes) {
        Map<FactStage, List<BusinessFactNode>> groups = new LinkedHashMap<>();
        for (FactStage stage : BusinessFactContract.PROJECT_STAGES) {
            groups.put(stage, new ArrayList<>());
        }
        for (BusinessFactNode node : ordered(nodes)) {
            if (node.getStage() != null) {
                groups.computeIfAbsent(node.getStage(), ignored -> new ArrayList<>()).add(node);
            }
        }
        return groups;
    }

    /** 同 nodeKey 的当前事实与历史版本归为一组，供页面在同一摘要卡内折叠展示。 */
    public static Map<String, List<BusinessFactNode>> nodeKeyGroups(List<BusinessFactNode> nodes) {
        Map<String, List<BusinessFactNode>> groups = new LinkedHashMap<>();
        for (BusinessFactNode node : ordered(nodes)) {
            if (node.getNodeKey() != null && !node.getNodeKey().isBlank()) {
                groups.computeIfAbsent(node.getNodeKey(), ignored -> new ArrayList<>()).add(node);
            }
        }
        return groups;
    }

    public static void validate(ProjectFactTrace trace) {
        Objects.requireNonNull(trace, "trace");
        if (trace.getProjectId() == null) {
            throw new IllegalArgumentException("项目事实追踪必须绑定 projectId");
        }
        Set<String> nodeIds = new HashSet<>();
        for (BusinessFactNode node : trace.getNodes() == null ? List.<BusinessFactNode>of() : trace.getNodes()) {
            BusinessFactContract.validateNode(node);
            if (!trace.getProjectId().equals(node.getProjectId())) {
                throw new IllegalArgumentException("事实节点 projectId 与项目不一致");
            }
            if (node.getNodeId() == null || node.getNodeId().isBlank()) {
                throw new IllegalArgumentException("事实节点必须包含 nodeId");
            }
            if (!nodeIds.add(node.getNodeId())) {
                throw new IllegalArgumentException("事实节点 nodeId 重复：" + node.getNodeId());
            }
        }
        for (ProjectFactTrace.SourceGap gap : trace.getSourceGaps() == null
                ? List.<ProjectFactTrace.SourceGap>of() : trace.getSourceGaps()) {
            validateGap(gap);
        }
    }

    private static void validateGap(ProjectFactTrace.SourceGap gap) {
        if (gap == null || gap.getNodeKey() == null || gap.getNodeKey().isBlank()
                || gap.getStage() == null || gap.getFactType() == null
                || gap.getFactType().getStage() != gap.getStage()
                || gap.getLabel() == null || gap.getLabel().isBlank()
                || gap.getReason() == null || gap.getReason().isBlank()) {
            throw new IllegalArgumentException("来源缺口必须包含匹配的 stage/factType、label 和 reason");
        }
    }
}
