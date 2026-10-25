package com.th4.edge.decoboon.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.components.lock.DistributedLock;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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
    private CompanyPaymentNodeRepository paymentNodeRepository;
    private CompanyConstructionPlanRepository constructionPlans;
    private CompanyConstructionStageRepository constructionStageRepository;
    private AtomicReference<CompanyPaymentPlan> paymentRef;
    private AtomicReference<CompanyConstructionPlan> constructionRef;
    private List<CompanyPaymentNode> paymentNodes;
    private List<CompanyConstructionStage> constructionStages;

    @BeforeEach
    void setUp() {
        TenantContext tenant = mock(TenantContext.class);
        DistributedLock lock = mock(DistributedLock.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(lock.tryLock(any(), eq(1000L), eq(30000L))).thenReturn(true);
        when(transactionTemplate.execute(org.mockito.ArgumentMatchers.<TransactionCallback<Object>>any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
        paymentPlans = mock(CompanyPaymentPlanRepository.class);
        paymentNodeRepository = mock(CompanyPaymentNodeRepository.class);
        constructionPlans = mock(CompanyConstructionPlanRepository.class);
        constructionStageRepository = mock(CompanyConstructionStageRepository.class);
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
        when(paymentPlans.findFirstByCompany_ComIdAndCodeOrderByVersionDesc(
                org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(paymentRef.get()));
        when(paymentPlans.findByCompany_ComIdAndCodeAndCurrentEnabledTrue(
                org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(paymentRef.get())
                        .filter(plan -> Boolean.TRUE.equals(plan.getCurrentEnabled())).map(List::of).orElse(List.of()));
        when(paymentPlans.findByCompany_ComIdAndCurrentEnabledTrue(100L))
                .thenAnswer(invocation -> Optional.ofNullable(paymentRef.get())
                        .filter(plan -> Boolean.TRUE.equals(plan.getCurrentEnabled())).map(List::of).orElse(List.of()));
        when(paymentPlans.findByCompany_ComIdAndStatusAndCurrentEnabledTrueOrderByDefaultPlanDescTemplateIdAsc(
                org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.eq(PlanTemplateStatus.Enabled)))
                .thenAnswer(invocation -> Optional.ofNullable(paymentRef.get())
                        .filter(plan -> plan.getStatus() == PlanTemplateStatus.Enabled
                                && Boolean.TRUE.equals(plan.getCurrentEnabled())).map(List::of).orElse(List.of()));
        when(paymentNodeRepository.save(any(CompanyPaymentNode.class))).thenAnswer(invocation -> {
            CompanyPaymentNode node = invocation.getArgument(0);
            node.setNodeId((long) paymentNodes.size() + 1);
            paymentNodes.add(node);
            return node;
        });
        when(paymentNodeRepository.findByPlan_TemplateIdOrderBySortAscNodeIdAsc(501L))
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
        when(constructionPlans.findFirstByCompany_ComIdAndCodeOrderByVersionDesc(
                org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(constructionRef.get()));
        when(constructionPlans.findByCompany_ComIdAndCodeAndCurrentEnabledTrue(
                org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(constructionRef.get())
                        .filter(plan -> Boolean.TRUE.equals(plan.getCurrentEnabled())).map(List::of).orElse(List.of()));
        when(constructionPlans.findByCompany_ComIdAndCurrentEnabledTrue(100L))
                .thenAnswer(invocation -> Optional.ofNullable(constructionRef.get())
                        .filter(plan -> Boolean.TRUE.equals(plan.getCurrentEnabled())).map(List::of).orElse(List.of()));
        when(constructionPlans.findByCompany_ComIdAndStatusAndCurrentEnabledTrueOrderByDefaultPlanDescTemplateIdAsc(
                org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.eq(PlanTemplateStatus.Enabled)))
                .thenAnswer(invocation -> Optional.ofNullable(constructionRef.get())
                        .filter(plan -> plan.getStatus() == PlanTemplateStatus.Enabled
                                && Boolean.TRUE.equals(plan.getCurrentEnabled())).map(List::of).orElse(List.of()));
        when(constructionStageRepository.save(any(CompanyConstructionStage.class))).thenAnswer(invocation -> {
            CompanyConstructionStage stage = invocation.getArgument(0);
            stage.setStageId((long) constructionStages.size() + 1);
            constructionStages.add(stage);
            return stage;
        });
        when(constructionStageRepository.findByPlan_TemplateIdOrderBySortAscStageIdAsc(601L))
                .thenAnswer(invocation -> List.copyOf(constructionStages));

        context = new CompanyPlanTemplateContext(tenant, baselines, paymentPlans, paymentNodeRepository,
                constructionPlans, constructionStageRepository, lock, transactionTemplate);
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
        assertThat(enabled.getCurrentEnabled()).isTrue();
        assertThat(enabled.getDefaultPlan()).isTrue();

        PaymentPlanTemplate resolved = context.resolvePaymentPlan(100L, paymentRef.get().getCode());
        assertThat(resolved.getSource()).isEqualTo(PlanTemplateSource.Company);
        assertThat(resolved.getTemplateId()).isEqualTo(501L);
    }

    @Test
    void buildsPaymentNextDraftWithoutChangingCurrentVersion() {
        CompanyPaymentPlan request = new CompanyPaymentPlan();
        request.setSourceBaselineCode("STANDARD_30_40_30");
        request.setName("企业标准收款");
        context.savePayment(boss, request);
        CompanyPaymentPlan publish = new CompanyPaymentPlan();
        publish.setTemplateId(501L);
        context.publishPayment(boss, publish);

        when(paymentPlans.save(any(CompanyPaymentPlan.class))).thenAnswer(invocation -> {
            CompanyPaymentPlan plan = invocation.getArgument(0);
            if (plan.getTemplateId() == null) plan.setTemplateId(502L);
            paymentRef.set(plan);
            return plan;
        });
        when(paymentNodeRepository.findByPlan_TemplateIdOrderBySortAscNodeIdAsc(502L)).thenReturn(List.of());
        CompanyPaymentPlan base = new CompanyPaymentPlan();
        base.setTemplateId(501L);
        PaymentPlanTemplate next = context.buildPayment(boss, base);

        assertThat(next.getVersion()).isEqualTo(2);
        assertThat(next.getStatus()).isEqualTo(PlanTemplateStatus.Draft);
        assertThat(next.getCurrentEnabled()).isFalse();
    }

    @Test
    void resolveWithoutExplicitDefaultIsRejected() {
        CompanyPaymentPlan enabled = new CompanyPaymentPlan();
        enabled.setTemplateId(501L);
        enabled.setCompany(boss.getCompany());
        enabled.setCode("PAY_NO_DEFAULT");
        enabled.setName("无默认");
        enabled.setStatus(PlanTemplateStatus.Enabled);
        enabled.setCurrentEnabled(Boolean.TRUE);
        enabled.setDefaultPlan(Boolean.FALSE);
        paymentRef.set(enabled);
        when(paymentPlans.findByCompany_ComIdAndStatusAndCurrentEnabledTrueOrderByDefaultPlanDescTemplateIdAsc(
                100L, PlanTemplateStatus.Enabled)).thenReturn(List.of(enabled));

        assertThatThrownBy(() -> context.resolvePaymentPlan(100L, null))
                .hasMessageContaining("请选择明确的付款计划模板");
    }

    @Test
    void publishingDefaultVersionKeepsOnlyOneCompanyDefaultAcrossCodes() {
        CompanyPaymentPlan oldDefault = new CompanyPaymentPlan();
        oldDefault.setTemplateId(501L);
        oldDefault.setCompany(boss.getCompany());
        oldDefault.setCode("PAY-A");
        oldDefault.setName("旧默认");
        oldDefault.setVersion(1);
        oldDefault.setStatus(PlanTemplateStatus.Enabled);
        oldDefault.setCurrentEnabled(Boolean.TRUE);
        oldDefault.setDefaultPlan(Boolean.TRUE);
        CompanyPaymentPlan otherDefault = new CompanyPaymentPlan();
        otherDefault.setTemplateId(502L);
        otherDefault.setCompany(boss.getCompany());
        otherDefault.setCode("PAY-B");
        otherDefault.setName("其他默认");
        otherDefault.setVersion(1);
        otherDefault.setStatus(PlanTemplateStatus.Enabled);
        otherDefault.setCurrentEnabled(Boolean.TRUE);
        otherDefault.setDefaultPlan(Boolean.TRUE);
        CompanyPaymentPlan draft = new CompanyPaymentPlan();
        draft.setTemplateId(503L);
        draft.setCompany(boss.getCompany());
        draft.setCode("PAY-A");
        draft.setName("新默认");
        draft.setVersion(2);
        draft.setStatus(PlanTemplateStatus.Draft);
        draft.setCurrentEnabled(Boolean.FALSE);
        draft.setDefaultPlan(Boolean.FALSE);
        paymentRef.set(draft);
        when(paymentPlans.findByCompany_ComIdAndTemplateId(100L, 503L)).thenReturn(Optional.of(draft));
        when(paymentPlans.findFirstByCompany_ComIdAndCodeOrderByVersionDesc(100L, "PAY-A"))
                .thenReturn(Optional.of(draft));
        when(paymentPlans.findByCompany_ComIdAndCodeAndCurrentEnabledTrue(100L, "PAY-A"))
                .thenReturn(List.of(oldDefault));
        when(paymentPlans.findByCompany_ComIdAndCurrentEnabledTrue(100L))
                .thenReturn(List.of(oldDefault, otherDefault));
        CompanyPaymentNode publishNode = new CompanyPaymentNode();
        publishNode.setCode("SIGN");
        publishNode.setName("签约款");
        publishNode.setRatio(java.math.BigDecimal.ONE);
        publishNode.setType(com.th4.edge.decoboon.finance.configure.ReceivableType.First);
        publishNode.setTriggerType(com.th4.edge.decoboon.plan.configure.PaymentTriggerType.ContractSigned);
        publishNode.setOffsetDays(0);
        publishNode.setSort(1);
        when(paymentNodeRepository.findByPlan_TemplateIdOrderBySortAscNodeIdAsc(503L))
                .thenReturn(List.of(publishNode));
        CompanyPaymentPlan command = new CompanyPaymentPlan();
        command.setTemplateId(503L);
        context.publishPayment(boss, command);
        assertThat(draft.getDefaultPlan()).isTrue();
        assertThat(oldDefault.getDefaultPlan()).isFalse();
        assertThat(otherDefault.getDefaultPlan()).isFalse();
    }

    @Test
    void repeatedSameBaselineUpgradeReturnsExistingDraft() {
        CompanyPaymentPlan request = new CompanyPaymentPlan();
        request.setSourceBaselineCode("STANDARD_30_40_30");
        request.setName("企业标准收款");
        context.savePayment(boss, request);
        CompanyPaymentPlan publish = new CompanyPaymentPlan();
        publish.setTemplateId(501L);
        context.publishPayment(boss, publish);

        when(paymentPlans.save(any(CompanyPaymentPlan.class))).thenAnswer(invocation -> {
            CompanyPaymentPlan plan = invocation.getArgument(0);
            if (plan.getTemplateId() == null) plan.setTemplateId(502L);
            paymentRef.set(plan);
            return plan;
        });
        when(paymentNodeRepository.findByPlan_TemplateIdOrderBySortAscNodeIdAsc(502L))
                .thenReturn(List.of());
        CompanyPaymentPlan command = new CompanyPaymentPlan();
        command.setTemplateId(501L);
        command.setSourceBaselineCode("STANDARD_30_40_30");
        PaymentPlanTemplate first = context.buildPayment(boss, command);
        PaymentPlanTemplate repeated = context.buildPayment(boss, command);
        assertThat(repeated.getTemplateId()).isEqualTo(first.getTemplateId());
        assertThat(repeated.getVersion()).isEqualTo(first.getVersion());
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
    void customConstructionKeepsDeclaredTotalDaysIndependentFromStageWeights() {
        CompanyConstructionPlan request = new CompanyConstructionPlan();
        request.setName("声明120天");
        request.setTotalDays(120);
        CompanyConstructionStage stage = new CompanyConstructionStage();
        stage.setCode("ONE");
        stage.setName("阶段");
        stage.setDefaultDays(100);
        stage.setAcceptanceMode(com.th4.edge.decoboon.plan.configure.StageAcceptanceMode.None);
        stage.setSort(1);
        request.setStages(List.of(stage));

        ConstructionPlanTemplate saved = context.saveConstruction(boss, request);

        assertThat(saved.getTotalDays()).isEqualTo(120);
        assertThat(saved.getStages()).hasSize(1);
    }

    @Test
    void customConstructionWithoutDeclaredTotalDaysUsesStageWeightsAsFallback() {
        CompanyConstructionPlan request = new CompanyConstructionPlan();
        request.setName("未声明总工期");
        request.setTotalDays(null);
        CompanyConstructionStage first = new CompanyConstructionStage();
        first.setCode("ONE");
        first.setName("阶段一");
        first.setDefaultDays(40);
        first.setAcceptanceMode(com.th4.edge.decoboon.plan.configure.StageAcceptanceMode.None);
        first.setSort(1);
        CompanyConstructionStage second = new CompanyConstructionStage();
        second.setCode("TWO");
        second.setName("阶段二");
        second.setDefaultDays(60);
        second.setAcceptanceMode(com.th4.edge.decoboon.plan.configure.StageAcceptanceMode.None);
        second.setSort(2);
        request.setStages(List.of(first, second));

        ConstructionPlanTemplate saved = context.saveConstruction(boss, request);

        assertThat(saved.getTotalDays()).isEqualTo(100);
    }

    @Test
    void explicitPlatformUpgradeCreatesConstructionDraftFromSelectedBaseline() {
        CompanyConstructionPlan request = new CompanyConstructionPlan();
        request.setSourceBaselineCode("RESIDENTIAL_SIMPLE_5");
        request.setName("企业精简施工");
        context.saveConstruction(boss, request);
        CompanyConstructionPlan publish = new CompanyConstructionPlan();
        publish.setTemplateId(601L);
        context.publishConstruction(boss, publish);

        when(constructionPlans.save(any(CompanyConstructionPlan.class))).thenAnswer(invocation -> {
            CompanyConstructionPlan plan = invocation.getArgument(0);
            if (plan.getTemplateId() == null) plan.setTemplateId(602L);
            constructionRef.set(plan);
            return plan;
        });
        constructionStages.clear();
        when(constructionStageRepository.findByPlan_TemplateIdOrderBySortAscStageIdAsc(602L))
                .thenAnswer(invocation -> List.copyOf(constructionStages));
        CompanyConstructionPlan command = new CompanyConstructionPlan();
        command.setTemplateId(601L);
        command.setSourceBaselineCode("RESIDENTIAL_STANDARD_8");
        ConstructionPlanTemplate upgraded = context.buildConstruction(boss, command);

        assertThat(upgraded.getVersion()).isEqualTo(2);
        assertThat(upgraded.getStatus()).isEqualTo(PlanTemplateStatus.Draft);
        assertThat(upgraded.getCurrentEnabled()).isFalse();
        assertThat(upgraded.getStages()).hasSize(8);
        assertThat(upgraded.getTotalDays()).isEqualTo(120);
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
        assertThat(enabled.getCurrentEnabled()).isTrue();
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
