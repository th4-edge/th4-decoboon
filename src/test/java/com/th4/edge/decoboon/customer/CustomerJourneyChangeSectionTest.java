package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.context.CustomerJourneyContext;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
import com.th4.edge.decoboon.customer.model.CustomerJourney;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.AcceptanceRoundRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 业主端增减项两态投影回归（DEC-068 / BR-041）。
 *
 * <p>契约：待审批不返回金额（避免未生效金额被业主当作已定），只给“审批中”与说明；
 * 已生效才返回带符号金额与生效时间。业主端不得再出现“客户确认”类事实。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerJourneyChangeSectionTest {

    @Mock private MeasureRepository measureRepository;
    @Mock private DesignRepository designRepository;
    @Mock private SpaceRepository spaceRepository;
    @Mock private DesignItemRepository designItemRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private QuoteLineRepository quoteLineRepository;
    @Mock private ProjectContractRepository contractRepository;
    @Mock private ContractChangeRepository changeRepository;
    @Mock private ContractChangeItemRepository changeItemRepository;
    @Mock private MaterialPlanRepository materialPlanRepository;
    @Mock private MaterialSelectionRepository selectionRepository;
    @Mock private ProjectStageRepository stageRepository;
    @Mock private ProjectReportRepository reportRepository;
    @Mock private AcceptanceRepository acceptanceRepository;
    @Mock private AcceptanceRoundRepository acceptanceRoundRepository;
    @Mock private ReceivableRepository receivableRepository;
    @Mock private CustomerFeedbackRepository feedbackRepository;

    private CustomerJourneyContext context;

    @BeforeEach
    void setUp() {
        context = new CustomerJourneyContext(measureRepository, designRepository, spaceRepository,
                designItemRepository, quoteRepository, quoteLineRepository, contractRepository,
                changeRepository, changeItemRepository, materialPlanRepository, selectionRepository,
                stageRepository, reportRepository, acceptanceRepository, acceptanceRoundRepository,
                receivableRepository, feedbackRepository, new QuoteViewContext());
    }

    @Test
    @DisplayName("待审批变更对业主只显示“审批中”，不返回金额；已生效才返回金额与生效时间")
    void shouldProjectChangeSectionWithTwoStatesForOwner() {
        stubEmptyJourney();
        ContractChange pending = change(31L, "BG-20261013-001", ContractChangeStatus.Pending, new BigDecimal("8010"), null);
        ContractChange effective = change(32L, "BG-20261012-001", ContractChangeStatus.CustomerConfirmed,
                new BigDecimal("6420"), LocalDateTime.of(2026, 10, 12, 16, 5));
        when(changeRepository.findByProject_ProjectIdAndCompany_ComIdOrderByChangeIdDesc(anyLong(), anyLong()))
                .thenReturn(List.of(pending, effective));
        when(changeItemRepository.findByChange_ChangeIdInOrderByChangeItemIdAsc(any()))
                .thenReturn(List.<ContractChangeItem>of());
        when(changeRepository.findImageRowsByChangeIds(any())).thenReturn(List.<Object[]>of());

        CustomerJourney journey = context.build(project());
        CustomerJourney.Section section = journey.getSections().stream()
                .filter(item -> "change".equals(item.getKey())).findFirst().orElseThrow();

        assertThat(section.getStatusName()).isEqualTo("审批中");
        assertThat(section.getFacts()).anySatisfy(fact -> {
            assertThat(fact.getKey()).isEqualTo("pending");
            assertThat(fact.getLabel()).isEqualTo("审批中");
            assertThat(fact.getValue()).isEqualTo("1");
        });

        CustomerJourney.Item pendingItem = item(section, "BG-20261013-001");
        assertThat(pendingItem.getStatusName()).isEqualTo("审批中");
        assertThat(pendingItem.getAmount()).isNull();
        assertThat(pendingItem.getDate()).isNull();
        assertThat(pendingItem.getFacts()).anySatisfy(fact ->
                assertThat(fact.getKey()).isEqualTo("pendingNotice"));
        assertThat(pendingItem.getFacts()).noneSatisfy(fact ->
                assertThat(fact.getKey()).contains("customerConfirmed"));

        CustomerJourney.Item effectiveItem = item(section, "BG-20261012-001");
        assertThat(effectiveItem.getStatusName()).isEqualTo("已生效");
        assertThat(effectiveItem.getAmount()).isEqualTo("6420.00");
        assertThat(effectiveItem.getFacts()).anySatisfy(fact -> {
            assertThat(fact.getKey()).isEqualTo("approvedAt");
            assertThat(fact.getValue()).startsWith("2026-10-12");
        });
    }

    @Test
    @DisplayName("报价金额缺失时保持暂不可确认，不能补 0")
    void missingQuoteAmountMustRemainUnconfirmable() throws Exception {
        Quote quote = new Quote();
        quote.setQuoteId(901L);
        quote.setNo("Q-MISSING-AMOUNT");
        quote.setVersion(1);
        quote.setStatus(com.th4.edge.decoboon.quote.configure.QuoteStatus.Accepted);
        quote.setAmount(null);
        when(quoteLineRepository.findByQuote(quote)).thenReturn(List.of());

        Method method = CustomerJourneyContext.class.getDeclaredMethod("quoteItem", Quote.class);
        method.setAccessible(true);
        CustomerJourney.Item item = (CustomerJourney.Item) method.invoke(context, quote);

        assertThat(item.getAmount()).isNull();
        assertThat(item.getHasSourceLines()).isFalse();
        assertThat(item.getFacts()).anySatisfy(fact -> {
            assertThat(fact.getKey()).isEqualTo("amount");
            assertThat(fact.getValue()).isEqualTo("暂不可确认");
        });
    }

    private CustomerJourney.Item item(CustomerJourney.Section section, String no) {
        return section.getItems().stream()
                .filter(item -> no.equals(item.getTitle()))
                .findFirst().orElseThrow();
    }

    private ContractChange change(Long id, String no, ContractChangeStatus status, BigDecimal amount, LocalDateTime approvedAt) {
        ContractChange change = new ContractChange();
        change.setChangeId(id);
        change.setNo(no);
        change.setReason("业主提出：客厅背景墙增加木饰面与灯带造型");
        change.setStatus(status);
        change.setAmount(amount);
        change.setApprovedAt(approvedAt);
        change.setType(ContractChangeType.Add);
        return change;
    }

    private Project project() {
        Company company = new Company();
        company.setComId(1L);
        Property property = new Property();
        property.setPropertyId(11L);
        property.setAddress("合肥市璟阳里 5-804");
        Project project = new Project();
        project.setProjectId(10L);
        project.setNo("PRJ-2026-0512");
        project.setName("璟阳里 5-804 全案装修");
        project.setCompany(company);
        project.setProperty(property);
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(60);
        return project;
    }

    private void stubEmptyJourney() {
        when(measureRepository.findByPropertyOrderByMeasureTimeDesc(any())).thenReturn(List.<Measure>of());
        when(designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(anyLong(), any()))
                .thenReturn(List.of());
        when(quoteRepository.findByProperty_PropertyIdOrderByQuoteIdDesc(anyLong())).thenReturn(List.of());
        when(contractRepository.findByProject(any())).thenReturn(java.util.Optional.<ProjectContract>empty());
        when(materialPlanRepository.findByProject(any())).thenReturn(List.of());
        when(selectionRepository.findByCompany_ComIdAndProject_ProjectIdOrderByVersionDesc(anyLong(), anyLong()))
                .thenReturn(List.of());
        when(stageRepository.findByProjectOrderBySort(any())).thenReturn(List.of());
        when(acceptanceRepository.findByProject_ProjectIdOrderByAcceptanceIdDesc(anyLong()))
                .thenReturn(List.<Acceptance>of());
        when(reportRepository.findByCompany_ComIdAndProject_ProjectIdAndVisibleToCustomerTrueOrderByCreateTime_DateTimeDesc(
                anyLong(), anyLong(), any(PageRequest.class))).thenReturn(new PageImpl<>(List.<ProjectReport>of()));
        when(receivableRepository.findByProjectOrderByDueDateAsc(any())).thenReturn(List.<Receivable>of());
        when(feedbackRepository.findByProject_ProjectIdAndCompany_ComIdOrderByFeedbackIdDesc(anyLong(), anyLong()))
                .thenReturn(List.<CustomerFeedback>of());
    }
}
