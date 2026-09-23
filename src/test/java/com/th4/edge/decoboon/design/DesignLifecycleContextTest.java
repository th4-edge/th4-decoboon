package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignLifecycleContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 设计方案状态随商务主链闭环推进的规则测试。 */
class DesignLifecycleContextTest {

    private final DesignRepository designRepository = mock(DesignRepository.class);
    private final DesignLifecycleContext context = new DesignLifecycleContext(designRepository);

    @Test
    void readyDesignShouldAdvanceToSelected() {
        Design design = design(11L, DesignStatus.Ready);
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> invocation.getArgument(0));

        context.markSelected(design);

        assertThat(design.getStatus()).isEqualTo(DesignStatus.Selected);
        verify(designRepository).save(design);
    }

    @Test
    void presentedDesignShouldAdvanceToSelected() {
        Design design = design(12L, DesignStatus.Presented);
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> invocation.getArgument(0));

        context.markSelected(design);

        assertThat(design.getStatus()).isEqualTo(DesignStatus.Selected);
        verify(designRepository).save(design);
    }

    @Test
    void selectedDesignMustNotChangeOrBeSavedAgain() {
        Design design = design(13L, DesignStatus.Selected);

        context.markSelected(design);

        assertThat(design.getStatus()).isEqualTo(DesignStatus.Selected);
        verify(designRepository, never()).save(any(Design.class));
    }

    @Test
    void archivedDesignMustNeverRegress() {
        Design design = design(14L, DesignStatus.Archived);

        context.markSelected(design);

        assertThat(design.getStatus()).isEqualTo(DesignStatus.Archived);
        verify(designRepository, never()).save(any(Design.class));
    }

    @Test
    void draftDesignMustNotBeAdvancedByQuoteChain() {
        Design design = design(15L, DesignStatus.Draft);

        context.markSelected(design);

        assertThat(design.getStatus()).isEqualTo(DesignStatus.Draft);
        verify(designRepository, never()).save(any(Design.class));
    }

    @Test
    void missingDesignOrIdShouldBeIgnored() {
        context.markSelected(null);
        context.markSelected(new Design());

        verify(designRepository, never()).save(any(Design.class));
    }

    private Design design(Long id, DesignStatus status) {
        Design design = new Design();
        design.setDesignId(id);
        design.setStatus(status);
        return design;
    }
}
