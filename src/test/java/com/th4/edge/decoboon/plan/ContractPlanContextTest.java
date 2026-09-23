package com.th4.edge.decoboon.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.plan.context.CompanyPlanTemplateContext;
import com.th4.edge.decoboon.plan.context.ContractPlanContext;
import com.th4.edge.decoboon.plan.context.PlanTemplateBaselineContext;
import com.th4.edge.decoboon.plan.model.ContractConstructionPlan;
import com.th4.edge.decoboon.plan.model.ContractConstructionStage;
import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
import com.th4.edge.decoboon.plan.repository.ContractConstructionPlanRepository;
import com.th4.edge.decoboon.plan.repository.ContractConstructionStageRepository;
import com.th4.edge.decoboon.plan.repository.ContractPaymentNodeRepository;
import com.th4.edge.decoboon.plan.repository.ContractPaymentPlanRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractPlanContextTest {

    private ContractPlanContext context;
    private ProjectContract contract;
    private List<ContractPaymentNode> savedNodes;
    private List<ContractConstructionStage> savedStages;

    @BeforeEach
    void setUp() {
        ContractPaymentPlanRepository paymentPlanRepository = mock(ContractPaymentPlanRepository.class);
        ContractPaymentNodeRepository paymentNodeRepository = mock(ContractPaymentNodeRepository.class);
        ContractConstructionPlanRepository constructionPlanRepository = mock(ContractConstructionPlanRepository.class);
        ContractConstructionStageRepository constructionStageRepository = mock(ContractConstructionStageRepository.class);
        AtomicReference<ContractPaymentPlan> paymentRef = new AtomicReference<>();
        AtomicReference<ContractConstructionPlan> constructionRef = new AtomicReference<>();
        savedNodes = new ArrayList<>();
        savedStages = new ArrayList<>();

        when(paymentPlanRepository.findByContract_ContractIdAndCompany_ComId(9L, 100L))
                .thenAnswer(invocation -> Optional.ofNullable(paymentRef.get()));
        when(constructionPlanRepository.findByContract_ContractIdAndCompany_ComId(9L, 100L))
                .thenAnswer(invocation -> Optional.ofNullable(constructionRef.get()));
        when(paymentPlanRepository.save(any(ContractPaymentPlan.class))).thenAnswer(invocation -> {
            ContractPaymentPlan plan = invocation.getArgument(0);
            plan.setPlanId(1L);
            paymentRef.set(plan);
            return plan;
        });
        when(constructionPlanRepository.save(any(ContractConstructionPlan.class))).thenAnswer(invocation -> {
            ContractConstructionPlan plan = invocation.getArgument(0);
            plan.setPlanId(2L);
            constructionRef.set(plan);
            return plan;
        });
        when(paymentNodeRepository.findByPlan_PlanIdOrderBySortAsc(1L)).thenAnswer(invocation -> savedNodes);
        when(constructionStageRepository.findByPlan_PlanIdOrderBySortAsc(2L)).thenAnswer(invocation -> savedStages);
        when(paymentNodeRepository.save(any(ContractPaymentNode.class))).thenAnswer(invocation -> {
            ContractPaymentNode node = invocation.getArgument(0);
            node.setNodeId((long) savedNodes.size() + 1);
            savedNodes.add(node);
            return node;
        });
        when(constructionStageRepository.save(any(ContractConstructionStage.class))).thenAnswer(invocation -> {
            ContractConstructionStage stage = invocation.getArgument(0);
            stage.setStageId((long) savedStages.size() + 1);
            savedStages.add(stage);
            return stage;
        });
        PlanTemplateBaselineContext baseline = new PlanTemplateBaselineContext(new ObjectMapper());
        baseline.load();
        CompanyPlanTemplateContext templates = mock(CompanyPlanTemplateContext.class);
        when(templates.resolvePaymentPlan(org.mockito.ArgumentMatchers.eq(100L), any()))
                .thenAnswer(invocation -> baseline.paymentPlan(invocation.getArgument(1)));
        when(templates.resolveConstructionPlan(org.mockito.ArgumentMatchers.eq(100L), any()))
                .thenAnswer(invocation -> baseline.constructionPlan(invocation.getArgument(1)));
        context = new ContractPlanContext(templates, paymentPlanRepository, paymentNodeRepository,
                constructionPlanRepository, constructionStageRepository);
        Company company = new Company();
        company.setComId(100L);
        contract = new ProjectContract();
        contract.setContractId(9L);
        contract.setCompany(company);
        contract.setStatus(ContractStatus.PendingSign);
        contract.setCurrentAmount(new BigDecimal("100000"));
        contract.setStartDate(LocalDate.of(2026, 9, 20));
        contract.setPlanEndDate(LocalDate.of(2027, 1, 17));
    }

    @Test
    void createsConstructionAndPaymentSnapshotsFromSelectedBaselines() {
        context.select(contract, "STANDARD_30_40_30", "RESIDENTIAL_STANDARD_8");

        assertThat(savedStages).hasSize(8);
        assertThat(savedStages.get(0).getCode()).isEqualTo("PREPARE");
        assertThat(savedStages.get(7).getCode()).isEqualTo("COMPLETION");
        assertThat(savedStages.get(0).getPlannedStart()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(savedStages.get(7).getPlannedEnd()).isEqualTo(LocalDate.of(2027, 1, 17));
        assertThat(savedNodes).hasSize(3);
        assertThat(savedNodes).extracting(ContractPaymentNode::getAmount)
                .containsExactly(new BigDecimal("30000.00"), new BigDecimal("40000.00"), new BigDecimal("30000.00"));
        assertThat(savedNodes.get(1).getTriggerStageCode()).isEqualTo("PREPARE");
        assertThat(savedNodes.get(2).getTriggerType().name()).isEqualTo("ProjectFinished");
    }
}
