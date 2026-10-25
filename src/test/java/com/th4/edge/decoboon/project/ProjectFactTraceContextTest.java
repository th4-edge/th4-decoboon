package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.common.configure.FactType;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.finance.configure.SettlementStatus;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.finance.configure.ReceiptStatus;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.context.ProjectFactTraceContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectFactTrace;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.GoodsReceiptRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRegionRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectFactTraceContextTest {

    @Mock private ProjectContractRepository contractRepository;
    @Mock private DesignRepository designRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private MeasureRepository measureRepository;
    @Mock private MeasureRegionRepository measureRegionRepository;
    @Mock private ProjectStageRepository stageRepository;
    @Mock private ProjectTaskRepository taskRepository;
    @Mock private ProjectReportRepository reportRepository;
    @Mock private MaterialSelectionRepository selectionRepository;
    @Mock private MaterialPlanRepository materialPlanRepository;
    @Mock private PurchaseOrderRepository purchaseRepository;
    @Mock private GoodsReceiptRepository goodsReceiptRepository;
    @Mock private ContractChangeRepository changeRepository;
    @Mock private AcceptanceRepository acceptanceRepository;
    @Mock private ReceivableRepository receivableRepository;
    @Mock private ReceiptRepository receiptRepository;
    @Mock private PayableRepository payableRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CustomerFeedbackRepository feedbackRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private ProjectCostContext costContext;

    private ProjectFactTraceContext context;
    private Project project;

    @BeforeEach
    void setUp() {
        context = new ProjectFactTraceContext(contractRepository, designRepository, quoteRepository,
                measureRepository, measureRegionRepository,
                stageRepository, taskRepository, reportRepository, selectionRepository, materialPlanRepository,
                purchaseRepository, goodsReceiptRepository, changeRepository, acceptanceRepository,
                receivableRepository, receiptRepository, payableRepository, paymentRepository, feedbackRepository,
                settlementRepository, costContext);
        project = project();
        when(contractRepository.findDetailedByProject(project)).thenReturn(java.util.Optional.of(contract(project)));
        when(designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(anyLong(), any()))
                .thenReturn(List.of());
        when(quoteRepository.findByDesign_DesignIdOrderByVersionDesc(anyLong())).thenReturn(List.of());
        when(quoteRepository.findByCompany_ComIdAndProperty_PropertyIdOrderByQuoteIdDesc(anyLong(), anyLong()))
                .thenReturn(List.of());
        when(measureRepository.findByCompany_ComIdAndProperty_PropertyIdOrderByMeasureTimeDesc(anyLong(), anyLong()))
                .thenReturn(List.<Measure>of());
        when(stageRepository.findDetailedByProjectOrderBySort(project)).thenReturn(List.of(stage(project)));
        when(taskRepository.findDetailedByProject(project)).thenReturn(List.of());
        when(reportRepository.findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(anyLong(), anyLong(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(selectionRepository.findByCompany_ComIdAndProject_ProjectIdOrderByVersionDesc(anyLong(), anyLong()))
                .thenReturn(List.of());
        when(materialPlanRepository.findByProject(project)).thenReturn(List.of());
        when(purchaseRepository.findByProjectOrderByPurchaseIdDesc(project)).thenReturn(List.of());
        when(goodsReceiptRepository.findByPurchase_Project_ProjectIdOrderByReceiptIdDesc(anyLong()))
                .thenReturn(List.of());
        when(changeRepository.findByProject_ProjectIdAndCompany_ComIdOrderByChangeIdDesc(anyLong(), anyLong()))
                .thenReturn(List.of());
        when(acceptanceRepository.findByProject_ProjectIdOrderByAcceptanceIdDesc(anyLong()))
                .thenReturn(List.of());
        when(receivableRepository.findByProjectOrderByDueDateAsc(project)).thenReturn(List.of());
        when(receiptRepository.findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(anyLong(), anyLong()))
                .thenReturn(List.of());
        when(payableRepository.findByProjectOrderByDueDateAsc(project)).thenReturn(List.of());
        when(paymentRepository.findByCompany_ComIdAndProject_ProjectIdOrderByPaidAtDesc(anyLong(), anyLong()))
                .thenReturn(List.of());
        when(feedbackRepository.findByProject_ProjectIdAndCompany_ComIdOrderByFeedbackIdDesc(anyLong(), anyLong()))
                .thenReturn(List.of());
        when(settlementRepository.findByProject_ProjectIdAndCompany_ComId(anyLong(), anyLong()))
                .thenReturn(java.util.Optional.empty());
        when(costContext.summarize(project)).thenReturn(new ProjectCostContext.CostSummary(
                new BigDecimal("100.00"), new BigDecimal("80.00"), 0));
    }

    @Test
    void shouldBuildCurrentFactsAndSourceGapsWithoutInternalCostForOrdinaryRole() {
        ProjectFactTrace trace = context.build(project, false);

        assertThat(trace.getProjectId()).isEqualTo(project.getProjectId());
        assertThat(trace.getDefaultViewMode().name()).isEqualTo("PHASE");
        assertThat(trace.getNodes()).extracting(node -> node.getFactType().name())
                .contains("Customer", "Property", "ProjectOrigin", "Quote", "QuoteAcceptance", "Contract", "Project", "Stage")
                .doesNotContain("Cost", "Payable", "Payment");
        assertThat(trace.getSourceGaps()).extracting(ProjectFactTrace.SourceGap::getFactType)
                .contains(FactType.Measure, FactType.Design);
        assertThat(trace.getNodes()).allSatisfy(node -> {
            assertThat(node.getProjectId()).isEqualTo(project.getProjectId());
            assertThat(node.getNodeKey()).isNotBlank();
            assertThat(node.getReturnPath()).isEqualTo("project:" + project.getProjectId());
            assertThat(node.getReturnAnchor()).isEqualTo("project-facts");
        });
    }

    @Test
    void shouldAddInternalCostOnlyForAuthorizedProjection() {
        ProjectFactTrace trace = context.build(project, true);

        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.Cost)
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.getCostComplete()).isTrue();
                    assertThat(node.getAmount()).isEqualByComparingTo("80.00");
                    assertThat(node.getVisibility().name()).isEqualTo("Internal");
                });
    }

    @Test
    void shouldFoldQuoteAndDesignVersionsUnderStableNodeKeys() {
        Design currentDesign = design(61L, 2, DesignStatus.Selected);
        Design oldDesign = design(60L, 1, DesignStatus.Archived);
        project.getQuote().setDesign(currentDesign);
        Quote oldQuote = quote(20L, 1, QuoteStatus.Superseded);
        oldQuote.setDesign(currentDesign);
        when(designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(anyLong(), any()))
                .thenReturn(List.of(currentDesign, oldDesign));
        when(quoteRepository.findByDesign_DesignIdOrderByVersionDesc(61L))
                .thenReturn(List.of(project.getQuote(), oldQuote));

        ProjectFactTrace trace = context.build(project, false);

        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.Design)
                .hasSize(2)
                .extracting(node -> node.getNodeKey()).containsOnly("Design:Design:property:11");
        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.Design
                        && Boolean.FALSE.equals(node.getCurrent()))
                .singleElement()
                .satisfies(node -> assertThat(node.getUncertainty()).contains("历史/替代"));

        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.Quote)
                .hasSize(2)
                .extracting(node -> node.getNodeKey()).containsOnly("Quote:Quote:design:61");
        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.Quote
                        && Boolean.TRUE.equals(node.getCurrent()))
                .singleElement()
                .satisfies(node -> assertThat(node.getObjectId()).isEqualTo(21L));
    }

    @Test
    void shouldFoldMaterialSelectionVersionsPerContractLine() {
        ContractLine line = new ContractLine();
        line.setLineId(71L);
        MaterialSelection oldSelection = selection(81L, line, 1);
        MaterialSelection currentSelection = selection(82L, line, 2);
        when(selectionRepository.findByCompany_ComIdAndProject_ProjectIdOrderByVersionDesc(anyLong(), anyLong()))
                .thenReturn(List.of(currentSelection, oldSelection));

        ProjectFactTrace trace = context.build(project, false);

        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.MaterialSelection)
                .hasSize(2)
                .extracting(node -> node.getNodeKey())
                .containsOnly("MaterialSelection:MaterialSelection:line:71");
        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.MaterialSelection
                        && Boolean.TRUE.equals(node.getCurrent()))
                .singleElement()
                .satisfies(node -> assertThat(node.getObjectId()).isEqualTo(82L));
    }

    @Test
    void shouldExposeSettlementProfitOnlyToAuthorizedProjection() {
        Settlement settlement = settlement();
        when(settlementRepository.findByProject_ProjectIdAndCompany_ComId(31L, 1L))
                .thenReturn(java.util.Optional.of(settlement));

        ProjectFactTrace internal = context.build(project, true);

        assertThat(internal.getNodes()).filteredOn(node -> node.getFactType() == FactType.ProfitSettlement)
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.getAmount()).isEqualByComparingTo("320.00");
                    assertThat(node.getProfitAmount()).isEqualByComparingTo("88.00");
                    assertThat(node.getGrossMargin()).isEqualByComparingTo("27.5");
                    assertThat(node.getCostComplete()).isTrue();
                    assertThat(node.getVisibility().name()).isEqualTo("Internal");
                });

        ProjectFactTrace ordinary = context.build(project, false);
        assertThat(ordinary.getNodes()).noneMatch(node -> node.getFactType() == FactType.ProfitSettlement);
    }

    @Test
    void shouldKeepProfitNullWhenSettlementSnapshotIsIncomplete() {
        Settlement settlement = settlement();
        settlement.setProfitAmount(null);
        settlement.setGrossMargin(null);
        when(settlementRepository.findByProject_ProjectIdAndCompany_ComId(31L, 1L))
                .thenReturn(java.util.Optional.of(settlement));

        ProjectFactTrace trace = context.build(project, true);

        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.ProfitSettlement)
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.getProfitAmount()).isNull();
                    assertThat(node.getGrossMargin()).isNull();
                    assertThat(node.getCostComplete()).isFalse();
                    assertThat(node.getUncertainty()).contains("成本未完整");
                });
    }

    @Test
    void shouldKeepReversedReceiptAsHistoryOutsideCurrentTotal() {
        Receipt active = receipt(91L, new BigDecimal("100.00"), ReceiptStatus.Active);
        Receipt reversed = receipt(90L, new BigDecimal("50.00"), ReceiptStatus.Reversed);
        when(receiptRepository.findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(1L, 31L))
                .thenReturn(List.of(active, reversed));

        ProjectFactTrace trace = context.build(project, false);

        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.Receipt)
                .hasSize(2)
                .extracting(node -> node.getNodeKey()).containsOnly("Receipt:Receipt:project:31");
        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.Receipt
                        && Boolean.TRUE.equals(node.getCurrent()))
                .singleElement()
                .satisfies(node -> assertThat(node.getAmount()).isEqualByComparingTo("100.00"));
        assertThat(trace.getNodes()).filteredOn(node -> node.getFactType() == FactType.Receipt
                        && Boolean.FALSE.equals(node.getCurrent()))
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.getAmount()).isEqualByComparingTo("50.00");
                    assertThat(node.getUncertainty()).contains("不参与当前累计");
                });
    }

    private Design design(Long designId, Integer version, DesignStatus status) {
        Design design = new Design();
        design.setDesignId(designId);
        design.setVersion(version);
        design.setStatus(status);
        design.setName("设计方案 V" + version);
        return design;
    }

    private Quote quote(Long quoteId, Integer version, QuoteStatus status) {
        Quote quote = new Quote();
        quote.setQuoteId(quoteId);
        quote.setNo("Q-" + quoteId);
        quote.setVersion(version);
        quote.setStatus(status);
        quote.setAmount(new BigDecimal("250.00"));
        return quote;
    }

    private MaterialSelection selection(Long selectionId, ContractLine line, Integer version) {
        MaterialSelection selection = new MaterialSelection();
        selection.setSelectionId(selectionId);
        selection.setSourceContractLine(line);
        selection.setVersion(version);
        selection.setTitle("选材");
        return selection;
    }

    private Settlement settlement() {
        Settlement settlement = new Settlement();
        settlement.setSettlementId(101L);
        settlement.setStatus(SettlementStatus.Confirmed);
        settlement.setFinalAmount(new BigDecimal("320.00"));
        settlement.setProfitAmount(new BigDecimal("88.00"));
        settlement.setGrossMargin(new BigDecimal("27.5"));
        settlement.setConfirmedAt(LocalDate.of(2026, 10, 22));
        return settlement;
    }

    private Receipt receipt(Long receiptId, BigDecimal amount, ReceiptStatus status) {
        Receipt receipt = new Receipt();
        receipt.setReceiptId(receiptId);
        receipt.setAmount(amount);
        receipt.setStatus(status);
        receipt.setReceivedAt(LocalDate.of(2026, 10, 20));
        return receipt;
    }

    private Project project() {
        Company company = new Company();
        company.setComId(1L);
        Customer customer = new Customer();
        customer.setCustId(12L);
        customer.setName("测试客户");
        Property property = new Property();
        property.setPropertyId(11L);
        property.setCommunity("测试小区");
        property.setAddress("测试小区 1-101");
        Quote quote = new Quote();
        quote.setQuoteId(21L);
        quote.setNo("Q-21");
        quote.setVersion(2);
        quote.setAmount(new BigDecimal("300.00"));
        quote.setAcceptedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        Project project = new Project();
        project.setProjectId(31L);
        project.setNo("P-31");
        project.setName("测试项目");
        project.setCompany(company);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setBusinessOrigin(BusinessOrigin.New);
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(40);
        project.setActualEndDate(LocalDate.of(2026, 10, 20));
        return project;
    }

    private ProjectContract contract(Project project) {
        ProjectContract contract = new ProjectContract();
        contract.setContractId(41L);
        contract.setNo("C-41");
        contract.setProject(project);
        contract.setBaselineAmount(new BigDecimal("300.00"));
        contract.setCurrentAmount(new BigDecimal("320.00"));
        contract.setStatus(ContractStatus.Effective);
        contract.setEffectiveAt(LocalDateTime.of(2026, 10, 2, 9, 0));
        return contract;
    }

    private ProjectStage stage(Project project) {
        ProjectStage stage = new ProjectStage();
        stage.setStageId(51L);
        stage.setProject(project);
        stage.setName("水电");
        stage.setStatus(StageStatus.Doing);
        stage.setProgress(30);
        return stage;
    }
}
