package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.common.configure.FactType;
import com.th4.edge.decoboon.common.model.BusinessFactContract;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.plan.repository.ContractPaymentNodeRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.context.ContractWorkbenchContext;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.ContractWorkbench;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRegionRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractFactTraceTest {

    @Test
    void shouldBuildBoundUpstreamAndDownstreamFactsWithoutChangingAmounts() {
        Fixture fixture = fixture(true);

        ContractWorkbench workbench = fixture.context.build(fixture.contract);

        assertThat(workbench.getUpstreamFacts()).extracting(node -> node.getFactType())
                .containsExactly(FactType.Customer, FactType.Property, FactType.Measure,
                        FactType.Design, FactType.Quote, FactType.Contract);
        assertThat(workbench.getUpstreamFacts()).filteredOn(node -> node.getFactType() == FactType.Quote)
                .singleElement().satisfies(node -> {
                    assertThat(node.getObjectId()).isEqualTo(100L);
                    assertThat(node.getVersion()).isEqualTo(2);
                    assertThat(node.getAmount()).isEqualByComparingTo("286400.00");
                });
        assertThat(workbench.getUpstreamFacts()).filteredOn(node -> node.getFactType() == FactType.Design)
                .singleElement().satisfies(node -> assertThat(node.getVersion()).isEqualTo(3));
        assertThat(workbench.getSourceGaps()).isEmpty();
        assertThat(workbench.getDownstreamFacts()).extracting(node -> node.getFactType())
                .contains(FactType.Project, FactType.MaterialPlan, FactType.Acceptance, FactType.Cost);
        assertThat(workbench.getContract().getCurrentAmount()).isEqualByComparingTo("286400.00");
        assertThat(workbench.getLineCount()).isEqualTo(3);
        assertThat(workbench.getLineAmount()).isEqualByComparingTo("286400.00");
    }

    @Test
    void shouldHideCostForDesignerAndDefinePreProjectBoundaryAndCarryoverLabel() {
        Fixture fixture = fixture(true);
        fixture.contract.setProject(null);
        fixture.contract.setBusinessOrigin(BusinessOrigin.Carryover);

        ContractWorkbench workbench = fixture.context.build(fixture.contract, false);

        assertThat(workbench.getDownstreamFacts()).extracting(node -> node.getFactType())
                .doesNotContain(FactType.Cost);
        assertThat(workbench.getUpstreamFacts()).filteredOn(node -> node.getFactType() == FactType.Contract)
                .singleElement().satisfies(node -> assertThat(node.getSummary()).isEqualTo("项目承接（接管基线）"));
        assertThat(workbench.getUpstreamFacts()).allSatisfy(node -> {
            assertThat(node.getProjectId()).isNull();
            assertThat(BusinessFactContract.isPreProjectFact(node)).isTrue();
        });
    }

    @Test
    void shouldExposeSourceGapWithoutInventingDesignOrMeasureFacts() {
        Fixture fixture = fixture(false);

        ContractWorkbench workbench = fixture.context.build(fixture.contract);

        assertThat(workbench.getUpstreamFacts()).extracting(node -> node.getFactType())
                .doesNotContain(FactType.Measure, FactType.Design);
        assertThat(workbench.getSourceGaps()).containsExactly("无系统量房记录", "无设计方案来源");
    }

    private Fixture fixture(boolean withSources) {
        ProjectContractContext contractContext = mock(ProjectContractContext.class);
        ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
        ContractPaymentNodeRepository paymentNodeRepository = mock(ContractPaymentNodeRepository.class);
        ProjectCostContext costContext = mock(ProjectCostContext.class);
        ProjectStageRepository stageRepository = mock(ProjectStageRepository.class);
        ContractChangeRepository changeRepository = mock(ContractChangeRepository.class);
        MaterialSelectionRepository selectionRepository = mock(MaterialSelectionRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        MeasureRegionRepository measureRegionRepository = mock(MeasureRegionRepository.class);
        MaterialPlanRepository materialPlanRepository = mock(MaterialPlanRepository.class);
        AcceptanceRepository acceptanceRepository = mock(AcceptanceRepository.class);

        CompanyFixture companyFixture = new CompanyFixture();
        Customer customer = new Customer();
        customer.setCustId(10L);
        customer.setName("张女士");
        Property property = new Property();
        property.setPropertyId(20L);
        property.setCommunity("示例小区");
        property.setAddress("8-1801");

        Design design = null;
        if (withSources) {
            design = new Design();
            design.setDesignId(30L);
            design.setName("现代简约方案");
            design.setVersion(3);
            design.setStatus(DesignStatus.Selected);
        }
        Quote quote = new Quote();
        quote.setQuoteId(100L);
        quote.setNo("Q-100");
        quote.setVersion(2);
        quote.setStatus(QuoteStatus.Accepted);
        quote.setAmount(new BigDecimal("286400.00"));
        quote.setAcceptedAt(LocalDateTime.of(2026, 10, 22, 10, 0));
        quote.setDesign(design);
        quote.setCompany(companyFixture.company);

        Project project = new Project();
        project.setProjectId(200L);
        project.setNo("P-200");
        project.setName("张女士项目");
        project.setContractAmount(new BigDecimal("286400.00"));
        project.setCompany(companyFixture.company);

        ProjectContract contract = new ProjectContract();
        contract.setContractId(300L);
        contract.setNo("C-300");
        contract.setCompany(companyFixture.company);
        contract.setCustomer(customer);
        contract.setProperty(property);
        contract.setQuote(quote);
        contract.setProject(project);
        contract.setStatus(ContractStatus.Effective);
        contract.setCurrentAmount(new BigDecimal("286400.00"));
        contract.setSignedAt(LocalDate.of(2026, 10, 22));
        contract.setCostComplete(true);

        when(contractContext.enrich(contract)).thenReturn(contract);
        when(receivableRepository.findByProjectOrderByDueDateAsc(project)).thenReturn(List.of());
        when(stageRepository.findByProjectOrderBySort(project)).thenReturn(List.of());
        when(changeRepository.findByContractOrderByChangeIdDesc(contract)).thenReturn(List.of());
        ContractLineRepository lineRepository = mock(ContractLineRepository.class);
        when(lineRepository.summarizeByContract(contract)).thenReturn(List.<Object[]>of(
                new Object[]{3L, new BigDecimal("286400.00")}));
        when(lineRepository.summarizeCostByContract(contract.getContractId())).thenReturn(List.<Object[]>of(
                new Object[]{3L, new BigDecimal("180000.00"), 0L}));
        when(selectionRepository.findByCompany_ComIdAndProperty_PropertyIdOrderByVersionDesc(companyFixture.company.getComId(), 20L))
                .thenReturn(List.of());
        when(costContext.summarize(project)).thenReturn(new ProjectCostContext.CostSummary(
                new BigDecimal("180000.00"), new BigDecimal("160000.00"), 0));
        when(materialPlanRepository.countByProject(project)).thenReturn(2L);
        when(acceptanceRepository.countByProject_ProjectId(200L)).thenReturn(1L);
        if (withSources) {
            Measure measure = new Measure();
            measure.setMeasureId(40L);
            measure.setStatus(MeasureStatus.Completed);
            measure.setCompletedAt(LocalDateTime.of(2026, 10, 18, 15, 0));
            when(measureRepository.findByCompany_ComIdAndProperty_PropertyIdOrderByMeasureTimeDesc(
                    companyFixture.company.getComId(), 20L)).thenReturn(List.of(measure));
            Space space = new Space();
            space.setSpaceId(50L);
            space.setName("客厅");
            MeasureRegion measured = new MeasureRegion();
            measured.setMeasure(measure);
            measured.setProperty(property);
            measured.setRoomType(com.th4.edge.decoboon.bim.configure.SpaceType.LivingRoom);
            measured.setName("客厅");
            measured.setAreaSource(AreaSource.Measured);
            measured.setArea(new BigDecimal("30"));
            MeasureRegion pending = new MeasureRegion();
            pending.setMeasure(measure);
            pending.setProperty(property);
            pending.setRoomType(com.th4.edge.decoboon.bim.configure.SpaceType.Bedroom);
            pending.setName("主卧");
            when(measureRegionRepository.findByCompany_ComIdAndMeasure_MeasureIdOrderBySortAscRegionIdAsc(
                    companyFixture.company.getComId(), 40L)).thenReturn(List.of(measured, pending));
        }

        ContractWorkbenchContext context = new ContractWorkbenchContext(paymentNodeRepository, contractContext,
                receivableRepository, costContext, stageRepository, changeRepository, lineRepository, selectionRepository,
                measureRepository, measureRegionRepository, materialPlanRepository, acceptanceRepository);
        return new Fixture(context, contract);
    }

    private record Fixture(ContractWorkbenchContext context, ProjectContract contract) {}

    private static final class CompanyFixture {
        private final com.th4.edge.tenant.user.model.Company company = new com.th4.edge.tenant.user.model.Company();
        private CompanyFixture() {
            company.setComId(1L);
        }
    }
}
