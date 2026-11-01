package com.th4.edge.decoboon.pack;

import com.th4.edge.decoboon.pack.context.PackageSummaryContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PackageSummaryContextTest {

    @Test
    void appliesBoundedLineAndSectionCountsWithoutLoadingItems() {
        PackageTemplateRepository repository = mock(PackageTemplateRepository.class);
        PackageTemplate configured = template(1L);
        PackageTemplate empty = template(2L);
        when(repository.summarizeLinesByCompany(9L))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 3L, 12L}));

        new PackageSummaryContext(repository).apply(List.of(configured, empty), 9L);

        assertThat(configured.getSectionCount()).isEqualTo(3);
        assertThat(configured.getLineCount()).isEqualTo(12);
        assertThat(configured.getReady()).isTrue();
        assertThat(empty.getSectionCount()).isZero();
        assertThat(empty.getLineCount()).isZero();
        assertThat(empty.getReady()).isFalse();
    }

    private static PackageTemplate template(Long id) {
        PackageTemplate template = new PackageTemplate();
        template.setTemplateId(id);
        return template;
    }
}
