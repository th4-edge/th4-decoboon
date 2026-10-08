package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.context.ContractBusinessStateContext;
import com.th4.edge.decoboon.project.model.ProjectContract;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 合同服务端状态视图测试。 */
class ContractBusinessStateContextTest {

    private final ContractBusinessStateContext context = new ContractBusinessStateContext();

    @Test
    void pendingSignShouldReturnSignAction() {
        ProjectContract contract = contract(ContractStatus.PendingSign);
        context.enrich(contract);
        assertThat(contract.getBusinessStateView().getStatus()).isEqualTo("PendingSign");
        assertThat(contract.getBusinessStateView().getNextAction().getKey()).isEqualTo("contract/sign");
        assertThat(contract.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Signed");
    }

    @Test
    void signedShouldReturnEffectiveActionNotProjectAction() {
        ProjectContract contract = contract(ContractStatus.Signed);
        context.enrich(contract);
        assertThat(contract.getBusinessStateView().getNextAction().getKey()).isEqualTo("contract/effective");
        assertThat(contract.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Effective");
    }

    @Test
    void effectiveShouldReturnProjectAction() {
        ProjectContract contract = contract(ContractStatus.Effective);
        context.enrich(contract);
        assertThat(contract.getBusinessStateView().getNextAction().getKey()).isEqualTo("project/open");
        assertThat(contract.getBusinessStateView().getNextAction().getLabel()).isEqualTo("查看项目");
    }

    @Test
    void changingShouldReturnChangeReviewAction() {
        ProjectContract contract = contract(ContractStatus.Changing);
        context.enrich(contract);
        assertThat(contract.getBusinessStateView().getNextAction().getKey()).isEqualTo("change/review");
        assertThat(contract.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Changed");
    }

    @Test
    void terminatedShouldReturnClosedReadonlyAction() {
        ProjectContract contract = contract(ContractStatus.Terminated);
        context.enrich(contract);
        assertThat(contract.getBusinessStateView().getNextAction().getKey()).isEqualTo("contract/closed-view");
        assertThat(contract.getBusinessStateView().getNextAction().getEnabled()).isFalse();
        assertThat(contract.getBusinessStateView().getStatusTone()).isEqualTo("blocked");
    }

    @Test
    void missingContractShouldBeIgnored() {
        context.enrich(null);
        context.enrich(new ProjectContract());
    }

    private ProjectContract contract(ContractStatus status) {
        ProjectContract contract = new ProjectContract();
        contract.setContractId(1L);
        contract.setStatus(status);
        return contract;
    }
}
