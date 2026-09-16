package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.TalkContext;
import com.th4.edge.decoboon.design.context.TalkPptContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.model.TalkSection;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.design.repository.TalkSectionRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TalkContextTest {

    private final TalkRepository talkRepository = mock(TalkRepository.class);
    private final TalkSectionRepository sectionRepository = mock(TalkSectionRepository.class);
    private final DesignRepository designRepository = mock(DesignRepository.class);
    private final DesignItemRepository itemRepository = mock(DesignItemRepository.class);
    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final RequirementRepository requirementRepository = mock(RequirementRepository.class);
    private final DesignContext designContext = mock(DesignContext.class);
    private final TalkPptContext talkPptContext = mock(TalkPptContext.class);
    private final TalkContext context = new TalkContext(talkRepository, sectionRepository,
            designRepository, itemRepository, spaceRepository, requirementRepository, designContext, talkPptContext);

    @Test
    void shouldRequireAtLeastTwoTalkableDesigns() {
        Design design = design(DesignStatus.Ready);
        Quote quote = quote(design);
        when(designRepository.countByCompany_ComIdAndCustomer_CustIdAndStatusIn(anyLong(), anyLong(), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> context.build(new Talk(), design, quote))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("至少完成两套");
    }

    @Test
    void shouldGenerateStructuredTalkSections() {
        Design design = design(DesignStatus.Ready);
        Quote quote = quote(design);
        when(designRepository.countByCompany_ComIdAndCustomer_CustIdAndStatusIn(anyLong(), anyLong(), any()))
                .thenReturn(2L);
        when(talkRepository.save(any(Talk.class))).thenAnswer(invocation -> {
            Talk talk = invocation.getArgument(0);
            talk.setTalkId(99L);
            return talk;
        });
        when(sectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(anyLong())).thenReturn(List.of());
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(anyLong())).thenReturn(List.of());
        when(requirementRepository.findByCompany_ComIdAndCustomer_CustIdOrderByRequirementIdDesc(anyLong(), anyLong()))
                .thenReturn(List.of());
        doNothing().when(designContext).apply(any(TalkSection.class), any(Talk.class));
        when(talkPptContext.export(any(Talk.class), any())).thenReturn("/upload/talk/test.pptx");

        Talk result = context.build(new Talk(), design, quote);

        ArgumentCaptor<List<TalkSection>> captor = ArgumentCaptor.forClass(List.class);
        verify(sectionRepository).saveAll(captor.capture());
        assertThat(result.getTalkId()).isEqualTo(99L);
        assertThat(captor.getValue()).extracting(TalkSection::getType)
                .contains(com.th4.edge.decoboon.design.configure.TalkSectionType.Requirement,
                        com.th4.edge.decoboon.design.configure.TalkSectionType.Investment,
                        com.th4.edge.decoboon.design.configure.TalkSectionType.End);
    }

    private Design design(DesignStatus status) {
        Company company = new Company();
        company.setComId(1L);
        Customer customer = new Customer();
        customer.setCustId(2L);
        Design design = new Design();
        design.setDesignId(3L);
        design.setName("现代简约方案");
        design.setStatus(status);
        design.setCompany(company);
        design.setCustomer(customer);
        return design;
    }

    private Quote quote(Design design) {
        Quote quote = new Quote();
        quote.setQuoteId(4L);
        quote.setDesign(design);
        return quote;
    }
}
