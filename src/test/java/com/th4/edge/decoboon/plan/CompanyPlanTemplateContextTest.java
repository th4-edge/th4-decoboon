package com.th4.edge.decoboon.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.plan.configure.PlanTemplateSource;
import com.th4.edge.decoboon.plan.configure.PlanTemplateStatus;
import com.th4.edge.decoboon.plan.context.CompanyPlanTemplateContext;
import com.th4.edge.decoboon.plan.context.PlanTemplateBaselineContext;
import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.decoboon.plan.model.CompanyConstructionStage;
import com.th4.edge.decoboon.plan.model.CompanyPaymentNode;
import com.th4.edge.decoboon.plan.model.CompanyPaymentPlan;
import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.decoboon.plan.repository.CompanyConstructionPlanRepository;
import com.th4.edge.decoboon.plan.repository.CompanyConstructionStageRepository;
import com.th4.edge.decoboon.plan.repository.CompanyPaymentNodeRepository;
import com.th4.edge.decoboon.plan.repository.CompanyPaymentPlanRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompanyPlanTemplateContextTest {

    private CompanyPlanTemplateContext context;
    private Employee boss;
    private Employee storeManager;
    private CompanyPaymentPlanRepository paymentPlans;
    private CompanyConstructionPlanRepository constructionPlans;
    private AtomicReference<CompanyPaymentPlan> paymentRef;
    private AtomicReference<CompanyConstructionPlan> constructionRef;
    private List<CompanyPaymentNode> paymentNodes;
    private List<CompanyConstructionStage> constructionStages;

    @BeforeEach
    void setUp() {
        TenantContext tenant = mock(TenantContext.class);
        paymentPlans = mock(CompanyPaymentPlanRepository.class);
        CompanyPaymentNodeRepository paymentNodesRepo = mock(CompanyPaymentNodeRepository.class);
        constructionPlans = mock(CompanyConstructionPlanRepository.class);
        CompanyConstructionStageRepository constructionStagesRepo = mock(CompanyConstructionStageRepository.class);
        PlanTemplateBaselineContext baselines = new PlanTemplateBaselineContext(new ObjectMapper());
        baselines.load();

        Company company = new Company();
        company.setComId(100L);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        storeManager = new Employee();
        storeManager.setEmpId(2L);
        storeManager.setRole(EmployeeRole.StoreManager);
        storeManager.setCompany(company);
        when(tenant.requireRole(eq(boss), eq(EmployeeRole.Boss), eq(EmployeeRole.StoreManager))).thenReturn(boss);
        when(tenant.requireRole(eq(storeManager), eq(EmployeeRole.Boss), eq(EmployeeRole.StoreManager))).thenReturn(storeManager);

        paymentRef = new AtomicReference<>();
        constructionRef = new AtomicReference<>();
        paymentNodes = new ArrayList<>();
        constructionStages = new ArrayList<>();

        when(paymentPlans.save(any(CompanyPaymentPlan.class))).thenAnswer(invocation -> {
            CompanyPaymentPlan plan = invocation.getArgument(0);
            if (plan.getTemplateId() == null) plan.setTemplateId(501L);
            paymentRef.set(plan);
            return plan;
        });
        when(paymentPlans.findByCompany_ComIdAndTemplateId(100L, 501L))
                .thenAnswer(invocation -> Optional.ofNullable(paymentRef.get()));
        when(paymentPlans.findByCompany_ComIdOrderByDefaultPlanDescSortAscTemplateIdAsc(100L))
                .thenAnswer(invocation -> paymentRef.get() == null ? List.of() : List.of(paymentRef.get()));
        when(paymentPlans.findFirstByCompany_ComIdAndStatusOrderByDefaultPlanDescSortAscTemplateIdAsc(100L, PlanTemplateStatus.Enabled))
                .thenAnswer(invocation -> Optional.ofNullable(paymentRef.get())
                        .filter(plan -> plan.getStatus() == PlanTemplateStatus.Enabled));
        when(paymentPlans.findByCompany_ComIdAndCodeAndStatus(org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(PlanTemplateStatus.Enabled)))
                .thenAnswer(invocation -> Optional.ofNullable(paymentRef.get())
                        .filter(plan -> plan.getStatus() == PlanTemplateStatus.Enabled));
        when(paymentNodesRepo.save(any(CompanyPaymentNode.class))).thenAnswer(invocation -> {
            CompanyPaymentNode node = invocation.getArgument(0);
            node.setNodeId((long) paymentNodes.size() + 1);
            paymentNodes.add(node);
            return node;
        });
        when(paymentNodesRepo.findByPlan_TemplateIdOrderBySortAscNodeIdAsc(501L))
                .thenAnswer(invocation -> List.copyOf(paymentNodes));

        when(constructionPlans.save(any(CompanyConstructionPlan.class))).thenAnswer(invocation -> {
            CompanyConstructionPlan plan = invocation.getArgument(0);
            if (plan.getTemplateId() == null) plan.setTemplateId(601L);
            constructionRef.set(plan);
            return plan;
        });
        when(constructionPlans.findByCompany_ComIdAndTemplateId(100L, 601L))
                .thenAnswer(invocation -> Optional.ofNullable(constructionRef.get()));
        when(constructionPlans.findByCompany_ComIdOrderByDefaultPlanDescSortAscTemplateIdAsc(100L))
                .thenAnswer(invocation -> constructionRef.get() == null ? List.of() : List.of(constructionRef.get()));
        when(constructionPlans.findFirstByCompany_ComIdAndStatusOrderByDefaultPlanDescSortAscTemplateIdAsc(100L, PlanTemplateStatus.Enabled))
                .thenAnswer(invocation -> Optional.ofNullable(constructionRef.get())
                        .filter(plan -> plan.getStatus() == PlanTemplateStatus.Enabled));
        when(constructionStagesRepo.save(any(CompanyConstructionStage.class))).thenAnswer(invocation -> {
            CompanyConstructionStage stage = invocation.getArgument(0);
            stage.setStageId((long) constructionStages.size() + 1);
            constructionStages.add(stage);
            return stage;
        });
        when(constructionStagesRepo.findByPlan_TemplateIdOrderBySortAscStageIdAsc(601L))
                .thenAnswer(invocation -> List.copyOf(constructionStages));

        context = new CompanyPlanTemplateContext(tenant, baselines, paymentPlans, paymentNodesRepo,
                constructionPlans, constructionStagesRepo);
    }

    @Test
    void copiesPublishesAndResolvesCompanyPaymentPlan() {
        CompanyPaymentPlan request = new CompanyPaymentPlan();
        request.setSourceBaselineCode("STANDARD_30_40_30");
        request.setName("企业标准收款");

        PaymentPlanTemplate draft = context.savePayment(boss, request);

        assertThat(draft.getTemplateId()).isEqualTo(501L);
        assertThat(draft.getSource()).isEqualTo(PlanTemplateSource.Company);
        assertThat(draft.getStatus()).isEqualTo(PlanTemplateStatus.Draft);
        assertThat(draft.getNodes()).hasSize(3);
        assertThat(paymentRef.get().getSourceBaselineCode()).isEqualTo("STANDARD_30_40_30");

        CompanyPaymentPlan publish = new CompanyPaymentPlan();
        publish.setTemplateId(501L);
        PaymentPlanTemplate enabled = context.publishPayment(boss, publish);
        assertThat(enabled.getStatus()).isEqualTo(PlanTemplateStatus.Enabled);
        assertThat(enabled.getDefaultPlan()).isTrue();

        PaymentPlanTemplate resolved = context.resolvePaymentPlan(100L, paymentRef.get().getCode());
        assertThat(resolved.getSource()).isEqualTo(PlanTemplateSource.Company);
        assertThat(resolved.getTemplateId()).isEqualTo(501L);
    }

    @Test
    void storeManagerCanCreateDraftPaymentPlan() {
        CompanyPaymentPlan request = new CompanyPaymentPlan();
        request.setSourceBaselineCode("STANDARD_30_40_30");
        request.setName("店长标准收款");

        PaymentPlanTemplate draft = context.savePayment(storeManager, request);

        assertThat(draft.getTemplateId()).isEqualTo(501L);
        assertThat(draft.getStatus()).isEqualTo(PlanTemplateStatus.Draft);
        assertThat(draft.getNodes()).hasSize(3);
    }

    @Test
    void copiesAndPublishesCompanyConstructionPlan() {
        CompanyConstructionPlan request = new CompanyConstructionPlan();
        request.setSourceBaselineCode("RESIDENTIAL_SIMPLE_5");
        request.setName("企业精简施工");

        ConstructionPlanTemplate draft = context.saveConstruction(boss, request);

        assertThat(draft.getTemplateId()).isEqualTo(601L);
        assertThat(draft.getSource()).isEqualTo(PlanTemplateSource.Company);
        assertThat(draft.getStatus()).isEqualTo(PlanTemplateStatus.Draft);
        assertThat(draft.getStages()).hasSize(5);
        assertThat(draft.getTotalDays()).isEqualTo(90);

        CompanyConstructionPlan publish = new CompanyConstructionPlan();
        publish.setTemplateId(601L);
        ConstructionPlanTemplate enabled = context.publishConstruction(boss, publish);
        assertThat(enabled.getStatus()).isEqualTo(PlanTemplateStatus.Enabled);
        assertThat(enabled.getDefaultPlan()).isTrue();
    }

    @Test
    void rejectsPaymentRatioNotEqualHundredPercent() {
        CompanyPaymentPlan request = new CompanyPaymentPlan();
        request.setName("错误比例模板");
        CompanyPaymentNode node = new CompanyPaymentNode();
        node.setCode("ONLY_50");
        node.setName("只收一半");
        node.setRatio(new java.math.BigDecimal("0.5"));
        request.setNodes(List.of(node));

        assertThatThrownBy(() -> context.savePayment(boss, request))
                .hasMessageContaining("付款计划比例合计必须为100%");
    }
}
