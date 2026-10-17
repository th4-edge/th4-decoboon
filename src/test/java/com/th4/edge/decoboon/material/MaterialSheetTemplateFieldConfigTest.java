package com.th4.edge.decoboon.material;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.material.context.MaterialSheetBaseline;
import com.th4.edge.decoboon.material.context.MaterialSheetTemplateContext;
import com.th4.edge.decoboon.material.repository.MaterialSheetTemplateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 品类模板必须把字段配置真正投影给前端和提交门禁，而不是只存在数据库 JSON 中。 */
class MaterialSheetTemplateFieldConfigTest {

    private final MaterialSheetTemplateRepository repository = mock(MaterialSheetTemplateRepository.class);
    private final MaterialSheetTemplateContext context = new MaterialSheetTemplateContext(
            repository, new MaterialSheetBaseline(), null, new ObjectMapper());

    @Test
    void tileShouldExposeRequiredMeasurementField() {
        when(repository.findFirstByCompany_ComIdAndSheetTypeAndPublishedVersionGreaterThanOrderByPublishedVersionDesc(
                anyLong(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(repository.findByCompany_ComIdAndSheetTypeOrderByVersionDesc(anyLong(), anyString()))
                .thenReturn(List.of());

        var required = context.requiredHeaderFields(1L, "Tile");

        assertThat(required).extracting(MaterialSheetTemplateContext.FieldRequirement::key)
                .contains("measureAt");
        var measure = required.stream().filter(field -> "measureAt".equals(field.key())).findFirst().orElseThrow();
        assertThat(measure.visible()).isTrue();
        assertThat(measure.required()).isTrue();
        assertThat(measure.label()).isNotBlank();
    }

    @Test
    void tileShouldExposeLineFieldMetadataForFrontend() {
        when(repository.findFirstByCompany_ComIdAndSheetTypeAndPublishedVersionGreaterThanOrderByPublishedVersionDesc(
                anyLong(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(repository.findByCompany_ComIdAndSheetTypeOrderByVersionDesc(anyLong(), anyString()))
                .thenReturn(List.of());

        var lineFields = context.fieldsForIssue(1L, "Tile", "line");

        assertThat(lineFields).extracting(MaterialSheetTemplateContext.FieldRequirement::key)
                .contains("locationName", "quantity", "unit");
        assertThat(lineFields.stream().filter(field -> "locationName".equals(field.key())).findFirst().orElseThrow().required())
                .isTrue();
    }
}
