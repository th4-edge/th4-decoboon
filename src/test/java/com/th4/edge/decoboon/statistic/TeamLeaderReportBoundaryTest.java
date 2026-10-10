package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.statistic.service.teamleader.report.support.Report;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TeamLeaderReportBoundaryTest {

    @Test
    void teamLeaderReportDoesNotExposeCostFields() {
        Properties properties = Report.class.getAnnotation(Properties.class);
        assertThat(properties).isNotNull();

        assertThat(allowed(properties, Quote.class)).doesNotContain("cost");
        assertThat(allowed(properties, Project.class)).doesNotContain("cost");
    }

    private String[] allowed(Properties properties, Class<?> model) {
        return Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(model))
                .findFirst()
                .map(AllowProperties::properties)
                .orElseThrow(() -> new AssertionError(model.getSimpleName() + " 未配置响应白名单"));
    }
}
