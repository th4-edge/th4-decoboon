package com.th4.edge.decoboon.ai;

import com.th4.edge.decoboon.ai.brief.model.AiBriefReport;
import com.th4.edge.decoboon.ai.brief.service.report.support.Report;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** AI 经营简报只能返回确定性事实、来源和影子用量，不返回平台成本。 */
class AiBriefSerializationContractTest {

    @Test
    void briefReportMustUseSafeWhitelist() {
        Properties properties = Report.class.getAnnotation(Properties.class);

        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(AiBriefReport.class);
            assertThat(Arrays.asList(item.properties()))
                    .contains("summary", "facts", "actions", "costStatus", "usage")
                    .doesNotContain("platformCost", "prompt", "company");
        });
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(AiBriefReport.AiBriefUsage.class);
            assertThat(Arrays.asList(item.properties()))
                    .contains("resourceUnits", "shadowBillableUnits")
                    .doesNotContain("platformCost");
        });
    }
}
