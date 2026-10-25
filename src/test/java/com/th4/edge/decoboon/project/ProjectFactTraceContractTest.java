package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.common.configure.FactStage;
import com.th4.edge.decoboon.common.configure.FactType;
import com.th4.edge.decoboon.common.configure.FactVisibility;
import com.th4.edge.decoboon.common.model.BusinessFactNode;
import com.th4.edge.decoboon.project.configure.ProjectFactViewMode;
import com.th4.edge.decoboon.project.model.ProjectFactTrace;
import com.th4.edge.decoboon.project.model.ProjectFactTraceContract;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectFactTraceContractTest {

    @Test
    void shouldFreezeEightStagesAndDefaultPhaseView() {
        ProjectFactTrace trace = new ProjectFactTrace();
        trace.setProjectId(10L);

        assertThat(ProjectFactTraceContract.phaseGroups(List.of()).keySet())
                .containsExactly(FactStage.Source, FactStage.PreSale, FactStage.Signing,
                        FactStage.Fulfillment, FactStage.Change, FactStage.Acceptance,
                        FactStage.Finance, FactStage.AfterSales);
        assertThat(trace.getDefaultViewMode()).isEqualTo(ProjectFactViewMode.PHASE);
    }

    @Test
    void shouldOrderFactsAndFilterOwnerVisibleFacts() {
        BusinessFactNode cost = node("cost-1", 10L, FactType.Cost,
                LocalDateTime.of(2026, 10, 1, 9, 0), FactVisibility.Internal);
        BusinessFactNode measure = node("measure-1", 10L, FactType.Measure,
                LocalDateTime.of(2026, 9, 1, 9, 0), FactVisibility.OwnerVisible);
        BusinessFactNode customer = node("customer-1", 10L, FactType.Customer,
                LocalDateTime.of(2026, 8, 1, 9, 0), FactVisibility.OwnerVisible);

        List<BusinessFactNode> ordered = ProjectFactTraceContract.ordered(List.of(cost, measure, customer));
        assertThat(ordered).extracting(BusinessFactNode::getNodeId)
                .containsExactly("customer-1", "measure-1", "cost-1");
        assertThat(ProjectFactTraceContract.ownerVisible(ordered))
                .extracting(BusinessFactNode::getNodeId)
                .containsExactly("customer-1", "measure-1");
    }

    @Test
    void shouldRejectCrossProjectAndDuplicateNodes() {
        BusinessFactNode foreign = node("foreign", 99L, FactType.Customer,
                LocalDateTime.of(2026, 10, 1, 9, 0), FactVisibility.Internal);
        ProjectFactTrace trace = new ProjectFactTrace();
        trace.setProjectId(10L);
        trace.setNodes(List.of(foreign));

        assertThatThrownBy(() -> ProjectFactTraceContract.validate(trace))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId");

        BusinessFactNode first = node("same", 10L, FactType.Customer,
                LocalDateTime.of(2026, 10, 1, 9, 0), FactVisibility.Internal);
        BusinessFactNode duplicate = node("same", 10L, FactType.Property,
                LocalDateTime.of(2026, 10, 2, 9, 0), FactVisibility.Internal);
        trace.setNodes(List.of(first, duplicate));
        assertThatThrownBy(() -> ProjectFactTraceContract.validate(trace))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nodeId");
    }

    @Test
    void shouldValidateSourceGapStageAndReason() {
        ProjectFactTrace trace = new ProjectFactTrace();
        trace.setProjectId(10L);
        ProjectFactTrace.SourceGap gap = new ProjectFactTrace.SourceGap();
        gap.setNodeKey("measure-missing");
        gap.setStage(FactStage.PreSale);
        gap.setFactType(FactType.Measure);
        gap.setLabel("量房");
        gap.setReason("未建立已完成量房记录");
        trace.setSourceGaps(List.of(gap));

        ProjectFactTraceContract.validate(trace);

        gap.setStage(FactStage.Finance);
        assertThatThrownBy(() -> ProjectFactTraceContract.validate(trace))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stage/factType");
    }

    @Test
    void shouldGroupCurrentAndHistoricalVersionsByNodeKey() {
        BusinessFactNode current = node("quote-current", 10L, FactType.Quote,
                LocalDateTime.of(2026, 10, 2, 9, 0), FactVisibility.OwnerVisible);
        BusinessFactNode history = node("quote-history", 11L, FactType.Quote,
                LocalDateTime.of(2026, 10, 1, 9, 0), FactVisibility.OwnerVisible);
        String nodeKey = "Quote:Quote:design:61";
        current.setNodeKey(nodeKey);
        history.setNodeKey(nodeKey);
        history.setCurrent(false);

        assertThat(ProjectFactTraceContract.nodeKeyGroups(List.of(current, history)).get(nodeKey))
                .extracting(BusinessFactNode::getNodeId)
                .containsExactly("quote-history", "quote-current");
        assertThat(history.getCurrent()).isFalse();
    }

    private BusinessFactNode node(String nodeId, Long projectId, FactType type,
                                  LocalDateTime businessTime, FactVisibility visibility) {
        BusinessFactNode node = new BusinessFactNode();
        node.setNodeId(nodeId);
        node.setNodeKey(nodeId);
        node.setStage(type.getStage());
        node.setFactType(type);
        node.setObjectType(type.name());
        node.setObjectId((long) nodeId.hashCode());
        node.setProjectId(projectId);
        node.setVersion(1);
        node.setCurrent(true);
        node.setBusinessTime(businessTime);
        node.setRecordedAt(businessTime.plusMinutes(1));
        node.setVisibility(visibility);
        node.setReturnPath("/project/detail");
        node.setReturnAnchor("fact-" + nodeId);
        return node;
    }
}
