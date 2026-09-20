package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignDeleteContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DesignDeleteContextTest {

    private final DesignContext designContext = mock(DesignContext.class);
    private final DesignRepository designRepository = mock(DesignRepository.class);
    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final DesignItemRepository itemRepository = mock(DesignItemRepository.class);
    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final TalkRepository talkRepository = mock(TalkRepository.class);
    private final DesignDeleteContext context = new DesignDeleteContext(
            designContext, designRepository, spaceRepository, itemRepository, quoteRepository, talkRepository);

    @Test
    void shouldDeleteDraftDesignWithSpacesAndItems() {
        Employee actor = employee();
        Design design = design(DesignStatus.Draft);
        Space space = new Space();
        DesignItem item = new DesignItem();
        when(designContext.requireDesign(3L, actor)).thenReturn(design);
        when(quoteRepository.countByDesign_DesignId(3L)).thenReturn(0L);
        when(talkRepository.existsByDesign_DesignId(3L)).thenReturn(false);
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(3L)).thenReturn(List.of(item));
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(3L)).thenReturn(List.of(space));

        Design deleted = context.delete(3L, actor);

        assertThat(deleted).isSameAs(design);
        verify(itemRepository).deleteAll(List.of(item));
        verify(spaceRepository).deleteAll(List.of(space));
        verify(designRepository).delete(design);
    }

    @Test
    void shouldRejectCompletedDesign() {
        Employee actor = employee();
        Design design = design(DesignStatus.Ready);
        when(designContext.requireDesign(3L, actor)).thenReturn(design);

        assertThatThrownBy(() -> context.delete(3L, actor))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("只有未完成的设计方案可以删除");

        verifyNoInteractions(quoteRepository, talkRepository, itemRepository, spaceRepository, designRepository);
    }

    private Design design(DesignStatus status) {
        Design design = new Design();
        design.setDesignId(3L);
        design.setName("现代简约方案");
        design.setStatus(status);
        return design;
    }

    private Employee employee() {
        Employee employee = new Employee();
        employee.setEmpId(7L);
        employee.setRole(EmployeeRole.Designer);
        return employee;
    }
}
