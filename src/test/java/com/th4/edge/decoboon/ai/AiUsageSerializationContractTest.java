package com.th4.edge.decoboon.ai;

import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.ai.service.usage.page.support.Page;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** AI 调用明细不得暴露企业对象、幂等键、请求标识或平台成本。 */
class AiUsageSerializationContractTest {

    @Test
    void usagePageMustUseSafeWhitelist() {
        Properties properties = Page.class.getAnnotation(Properties.class);

        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(AiUsageCall.class);
            assertThat(Arrays.asList(item.properties()))
                    .contains("callId", "allocationStore", "resourceUnits", "shadowBillableUnits")
                    .doesNotContain("company", "idempotencyKey", "requestId", "platformCost");
        });
    }
}
