package com.th4.edge.decoboon.document;

import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.document.service.quote.build.support.Build;
import com.th4.edge.decoboon.document.service.record.page.support.Page;
import com.th4.edge.decoboon.document.service.reprint.save.support.Save;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** 防止打印归档接口重新序列化完整报价、项目或员工关联树。 */
class DocumentResponseContractTest {

    private static final Set<String> ARCHIVE_FIELDS = Set.of(
            "documentId", "type", "audience", "businessNo", "templateVersion", "path",
            "sha256", "fileName", "contentType", "size", "generatedAt", "status",
            "url", "urlExpiresAt"
    );

    @Test
    void quoteBuildOnlyExposesArchiveFields() {
        assertArchiveOnly(Build.class);
    }

    @Test
    void reprintOnlyExposesArchiveFields() {
        assertArchiveOnly(Save.class);
    }

    @Test
    void recordPageOnlyExposesArchiveFields() {
        assertArchiveOnly(Page.class);
    }

    private void assertArchiveOnly(Class<?> providerClass) {
        Properties properties = AnnotationUtils.findAnnotation(providerClass, Properties.class);
        assertThat(properties).as("Provider must declare @Properties").isNotNull();
        AllowProperties documentAllow = Arrays.stream(properties.allow())
                .filter(allow -> allow.model() == DocumentRecord.class)
                .findFirst()
                .orElseThrow();
        assertThat(Set.of(documentAllow.properties()))
                .as(providerClass.getSimpleName() + " DocumentRecord response fields")
                .containsExactlyInAnyOrderElementsOf(ARCHIVE_FIELDS)
                .doesNotContain("quote", "project", "reprintOf", "generatedBy");
    }
}
