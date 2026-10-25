package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.common.model.BusinessFactNode;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectFactTrace;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectFactTracePropertiesTest {

    @Test
    void projectInfoShouldExposeFactTraceAndNodeFields() {
        Properties properties = com.th4.edge.decoboon.project.service.projects.info.support.Info.class
                .getAnnotation(Properties.class);
        assertThat(properties).isNotNull();

        assertThat(allowed(properties, Project.class)).contains("factTrace");
        assertThat(allowed(properties, ProjectFactTrace.class)).contains("nodes", "sourceGaps", "defaultViewMode");
        assertThat(allowed(properties, BusinessFactNode.class)).contains(
                "nodeId", "nodeKey", "stage", "factType", "projectId", "version",
                "businessTime", "recordedAt", "amount", "costComplete", "profitAmount", "grossMargin", "visibility",
                "navigationPath", "returnPath", "returnAnchor");
    }

    @Test
    void projectInfoRootMustNotExposeCostFields() {
        Properties properties = com.th4.edge.decoboon.project.service.projects.info.support.Info.class
                .getAnnotation(Properties.class);
        assertThat(properties).isNotNull();

        assertThat(allowed(properties, Project.class))
                .doesNotContain("cost", "actualCost", "grossProfit", "grossMargin", "costComplete");
        assertThat(allowed(properties, Quote.class)).doesNotContain("cost", "costComplete");
    }

    private List<String> allowed(Properties properties, Class<?> model) {
        return Arrays.stream(properties.allow())
                .filter(allow -> allow.model().equals(model))
                .flatMap(allow -> Arrays.stream(allow.properties()))
                .toList();
    }
}
