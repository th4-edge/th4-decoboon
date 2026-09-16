package com.th4.edge.decoboon.catalog.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 报价项目拼音首字母回归测试。 */
class PinyinContextTest {

    private final PinyinContext context = new PinyinContext();

    @Test
    void shouldGenerateExpectedInitials() {
        assertThat(context.initials("轻钢龙骨石膏板吊平顶")).isEqualTo("QGLGSGBDPD");
    }

    @Test
    void shouldTruncateLongInitialsToDatabaseWidth() {
        String longName = "超长项目名称".repeat(20);
        assertThat(context.initials(longName)).hasSize(64);
    }
}
