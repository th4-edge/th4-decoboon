package com.th4.edge.decoboon.common;

import com.th4.edge.decoboon.common.configure.FactStage;
import com.th4.edge.decoboon.common.configure.FactType;
import com.th4.edge.decoboon.common.configure.FactVisibility;
import com.th4.edge.decoboon.common.model.BusinessFactContract;
import com.th4.edge.decoboon.common.model.BusinessFactNode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessFactContractTest {

    @Test
    void projectStagesShouldUseFrozenBusinessOrder() {
        assertThat(Arrays.stream(FactStage.values()).sorted(java.util.Comparator.comparingInt(FactStage::getOrder)))
                .extracting(FactStage::name)
                .containsExactly("Source", "PreSale", "Signing", "Fulfillment",
                        "Change", "Acceptance", "Finance", "AfterSales");
        assertThat(BusinessFactContract.PROJECT_STAGES.stream().map(FactStage::getName).toList())
                .containsExactly("来源", "售前", "签约", "履约", "变更", "验收", "财务", "售后");
    }

    @Test
    void coreFactsShouldBelongToExpectedStageAndStayOrdered() {
        assertThat(FactType.Customer.getStage()).isEqualTo(FactStage.Source);
        assertThat(FactType.Measure.getStage()).isEqualTo(FactStage.PreSale);
        assertThat(FactType.Design.getStage()).isEqualTo(FactStage.PreSale);
        assertThat(FactType.Quote.getStage()).isEqualTo(FactStage.PreSale);
        assertThat(FactType.Contract.getStage()).isEqualTo(FactStage.Signing);
        assertThat(FactType.Project.getStage()).isEqualTo(FactStage.Fulfillment);
        assertThat(FactType.ContractChange.getStage()).isEqualTo(FactStage.Change);
        assertThat(FactType.Acceptance.getStage()).isEqualTo(FactStage.Acceptance);
        assertThat(FactType.Receivable.getStage()).isEqualTo(FactStage.Finance);
        assertThat(FactType.Delivery.getStage()).isEqualTo(FactStage.AfterSales);

        assertThat(Arrays.stream(FactType.values())
                .filter(type -> type.getStage() == FactStage.PreSale)
                .sorted(java.util.Comparator.comparingInt(FactType::getOrder)))
                .extracting(FactType::name)
                .containsExactly("Measure", "Design", "Quote");
    }

    @Test
    void contractUpstreamAndStableSortShouldBeFrozen() {
        assertThat(BusinessFactContract.CONTRACT_UPSTREAM_TYPES)
                .containsExactly(FactType.Measure, FactType.Design, FactType.Quote, FactType.Contract);

        BusinessFactNode later = node(FactType.Quote, 30L, "2026-10-22T10:00:00");
        BusinessFactNode earlier = node(FactType.Measure, 20L, "2026-10-21T10:00:00");
        BusinessFactNode sameTimeLowerId = node(FactType.Measure, 10L, "2026-10-21T10:00:00");
        assertThat(List.of(later, sameTimeLowerId, earlier).stream().sorted(BusinessFactContract.stableOrder()))
                .extracting(BusinessFactNode::getObjectId)
                .containsExactly(10L, 20L, 30L);
        assertThat(BusinessFactContract.NO_DESIGN_SOURCE).isEqualTo("无设计方案来源");
        assertThat(BusinessFactContract.CARRYOVER_SOURCE).isEqualTo("项目承接（接管基线）");
    }

    @Test
    void factNodeShouldCarryVersionSourceNavigationVisibilityAndUncertainty() {
        assertThat(Arrays.stream(BusinessFactNode.class.getDeclaredFields()).map(java.lang.reflect.Field::getName))
                .contains("nodeId", "nodeKey", "stage", "factType", "objectType", "objectId", "projectId", "version",
                        "current", "status", "statusName", "businessTime", "recordedAt", "actorName",
                        "title", "summary", "sourceLabel", "amount", "quantity", "progress", "costComplete",
                        "uncertainty", "parentNodeId", "relations", "visibility", "navigationPath",
                        "returnPath", "returnAnchor");
        assertThat(FactVisibility.Internal.name()).isEqualTo("Internal");
        assertThat(FactVisibility.OwnerVisible.name()).isEqualTo("OwnerVisible");
    }

    @Test
    void factNodeValidationShouldRequireProjectAndNavigationBinding() {
        BusinessFactNode valid = node(FactType.Quote, 30L, "2026-10-22T10:00:00");
        assertThatCode(() -> BusinessFactContract.validateNode(valid)).doesNotThrowAnyException();

        BusinessFactNode noProject = node(FactType.Quote, 30L, "2026-10-22T10:00:00");
        noProject.setProjectId(null);
        assertThatThrownBy(() -> BusinessFactContract.validateNode(noProject))
                .isInstanceOf(IllegalArgumentException.class);

        BusinessFactNode preProject = node(FactType.Contract, 30L, "2026-10-22T10:00:00");
        preProject.setProjectId(null);
        preProject.setUncertainty("项目尚未建立，事实按合同绑定关系展示");
        assertThatCode(() -> BusinessFactContract.validateNode(preProject)).doesNotThrowAnyException();
        assertThat(BusinessFactContract.isPreProjectFact(preProject)).isTrue();

        BusinessFactNode noReturnPath = node(FactType.Quote, 30L, "2026-10-22T10:00:00");
        noReturnPath.setReturnPath(null);
        assertThatThrownBy(() -> BusinessFactContract.validateNode(noReturnPath))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private BusinessFactNode node(FactType type, Long objectId, String businessTime) {
        BusinessFactNode node = new BusinessFactNode();
        node.setStage(type.getStage());
        node.setFactType(type);
        node.setNodeId(type.name() + "-" + objectId);
        node.setNodeKey(type.name() + ":" + objectId + ":1");
        node.setObjectId(objectId);
        node.setObjectType(type.name());
        node.setProjectId(900L);
        node.setVersion(1);
        node.setNavigationPath("/quote/" + objectId);
        node.setReturnPath("/project/900/facts");
        node.setReturnAnchor("phase-pre-sale");
        node.setBusinessTime(java.time.LocalDateTime.parse(businessTime));
        return node;
    }
}
