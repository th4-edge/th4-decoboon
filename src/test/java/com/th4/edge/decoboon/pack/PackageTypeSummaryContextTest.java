package com.th4.edge.decoboon.pack;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.pack.context.PackageTypeSummaryContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageSectionRepository;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PackageTypeSummaryContextTest {

    @Test
    void aggregatesSectionAndPackageTypeCountsFromDatabaseRows() {
        PackageLineRepository repository = mock(PackageLineRepository.class);
        PackageSectionRepository sectionRepository = mock(PackageSectionRepository.class);
        PackageTemplate template = new PackageTemplate();
        template.setTemplateId(9L);
        Company company = new Company();
        company.setComId(7L);
        template.setCompany(company);
        when(sectionRepository.findByCompany_ComIdOrderByPriorityAscGroupIdAsc(7L)).thenReturn(List.of(
                section(11L, "客厅"), section(12L, "厨房"), section(13L, "卫生间")
        ));
        when(repository.summarizeTypeCountsByTemplateId(9L)).thenReturn(List.of(
                new Object[]{11L, "Construction", 2L, 20, 10},
                new Object[]{11L, "Material", 3L, 30, 15},
                new Object[]{12L, "Material", 1L, 12.5, 5}
        ));

        new PackageTypeSummaryContext(repository, sectionRepository).apply(template);

        assertThat(template.getTypeCounts())
                .containsEntry(ItemType.Construction, 2)
                .containsEntry(ItemType.Material, 4);
        assertThat(template.getSectionTypeCounts().get(11L))
                .containsEntry(ItemType.Construction, 2)
                .containsEntry(ItemType.Material, 3);
        assertThat(template.getSectionLineCounts())
                .containsEntry(11L, 5)
                .containsEntry(12L, 1)
                .containsEntry(13L, 0);
        assertThat(template.getSectionReferenceAmounts())
                .containsEntry(11L, new java.math.BigDecimal("50.00"))
                .containsEntry(12L, new java.math.BigDecimal("12.50"))
                .containsEntry(13L, new java.math.BigDecimal("0.00"));
        assertThat(template.getSectionTypeReferenceAmounts().get(11L))
                .containsEntry(ItemType.Construction, new java.math.BigDecimal("20.00"))
                .containsEntry(ItemType.Material, new java.math.BigDecimal("30.00"));
        assertThat(template.getReferenceAmount()).isEqualByComparingTo("62.50");
        assertThat(template.getReferenceCost()).isEqualByComparingTo("30.00");
        assertThat(template.getLineCount()).isEqualTo(6);
        assertThat(template.getSectionCount()).isEqualTo(2);
        assertThat(template.getReady()).isTrue();
    }

    private PackageSection section(Long id, String name) {
        PackageSection section = new PackageSection();
        section.setGroupId(id);
        section.setName(name);
        return section;
    }
}
