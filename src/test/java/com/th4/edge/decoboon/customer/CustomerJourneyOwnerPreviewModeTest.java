package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustomerFeedbackStatus;
import com.th4.edge.decoboon.customer.configure.JourneyViewMode;
import com.th4.edge.decoboon.customer.context.CustomerJourneyContext;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.model.CustomerJourney;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.AcceptanceRoundRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 业主旅程 OwnerPreview 投影契约：同源字段、真实待办、P0 跳过反馈。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerJourneyOwnerPreviewModeTest {

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
                receivableRepository, feedbackRepository);
        stubEmptyJourney();
        when(feedbackRepository.findByProject_ProjectIdAndCompany_ComIdOrderByFeedbackIdDesc(anyLong(), anyLong()))
                .thenReturn(List.of(feedback()));
    }

    @Test
    void feedbackCapabilityMustNotBeCountedAsPendingAction() {
        CustomerJourney owner = context.build(project(), JourneyViewMode.Owner);
        CustomerJourney preview = context.build(project(), JourneyViewMode.EmployeeOwnerPreview);

        assertThat(owner.getPreviewMeta()).isNull();
        assertThat(owner.getActions()).isEmpty();
        assertThat(owner.getPendingActionCount()).isZero();
        assertThat(owner.getSections()).extracting(CustomerJourney.Section::getKey).contains("feedback");

        assertThat(preview.getActions()).isEmpty();
        assertThat(preview.getPendingActionCount()).isZero();
        assertThat(preview.getOpenFeedbackCount()).isZero();
        assertThat(preview.getSections()).extracting(CustomerJourney.Section::getKey)
                .doesNotContain("feedback")
                .containsExactlyElementsOf(owner.getSections().stream()
                        .map(CustomerJourney.Section::getKey)
                        .filter(key -> !"feedback".equals(key))
                        .toList());
        assertThat(preview.getPreviewMeta()).isNotNull();
        assertThat(preview.getPreviewMeta().getMode()).isEqualTo("EmployeeOwnerPreview");
        assertThat(preview.getPreviewMeta().getReadOnly()).isTrue();
        assertThat(preview.getPreviewMeta().getNotice())
                .isEqualTo("业主视角预览 · 只读，不代表业主已查看或确认");
        assertThat(preview.getPreviewMeta().getAsOf()).isEqualTo(preview.getAsOf());

        assertThat(preview.getProjectId()).isEqualTo(owner.getProjectId());
        assertThat(preview.getProjectNo()).isEqualTo(owner.getProjectNo());
        assertThat(preview.getProjectName()).isEqualTo(owner.getProjectName());
        assertThat(preview.getProjectStatus()).isEqualTo(owner.getProjectStatus());
        assertThat(preview.getProjectStatusName()).isEqualTo(owner.getProjectStatusName());
        assertThat(preview.getProgress()).isEqualTo(owner.getProgress());
        assertThat(preview.getCurrentStageName()).isEqualTo(owner.getCurrentStageName());
        assertThat(preview.getPropertyAddress()).isEqualTo(owner.getPropertyAddress());
        assertThat(preview.getVendor()).usingRecursiveComparison().isEqualTo(owner.getVendor());
        assertThat(preview.getTeam()).usingRecursiveComparison().isEqualTo(owner.getTeam());

        for (CustomerJourney.Section previewSection : preview.getSections()) {
            CustomerJourney.Section ownerSection = owner.getSections().stream()
                    .filter(section -> previewSection.getKey().equals(section.getKey()))
                    .findFirst().orElseThrow();
            assertThat(previewSection).usingRecursiveComparison().isEqualTo(ownerSection);
        }

        verify(feedbackRepository, times(1))
                .findByProject_ProjectIdAndCompany_ComIdOrderByFeedbackIdDesc(anyLong(), anyLong());
    }

    private CustomerFeedback feedback() {
        CustomerFeedback feedback = new CustomerFeedback();
        feedback.setFeedbackId(901L);
        feedback.setTitle("客厅收口建议");
        feedback.setContent("希望现场再确认收口方式");
        feedback.setStatus(CustomerFeedbackStatus.Submitted);
        return feedback;
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
        when(receivableRepository.findByProjectOrderByDueDateAsc(any())).thenReturn(List.of());
    }

    private Project project() {
        Company company = new Company();
        company.setComId(1L);
        Property property = new Property();
        property.setPropertyId(11L);
        property.setAddress("合肥市测试小区 1-101");
        Project project = new Project();
        project.setProjectId(10L);
        project.setNo("PRJ-PREVIEW-001");
        project.setName("预览投影测试项目");
        project.setCompany(company);
        project.setProperty(property);
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(38);
        return project;
    }
}
