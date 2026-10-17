package com.th4.edge.decoboon.material;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.material.context.MaterialSheetBaseline;
import com.th4.edge.decoboon.material.context.MaterialSheetTemplateContext;
import com.th4.edge.framework.exception.DefinedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 主材下单模板"受控个性化"守卫（BR-044）。
 *
 * <p>个性化只碰呈现与编排：字段键只能取平台白名单，数量/价格/成本/权限口径为红线。
 * 平台基线本身也必须自检，避免基线里写出白名单外的键。</p>
 */
class MaterialSheetTemplateWhitelistTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MaterialSheetTemplateContext context =
            new MaterialSheetTemplateContext(null, null, null, objectMapper);
    private final MaterialSheetBaseline baseline = new MaterialSheetBaseline();

    @Test
    void baselineTileShouldUseWhitelistedKeysOnly() throws Exception {
        MaterialSheetBaseline.Preset tile = baseline.tile();
        assertThat(tile.code()).isEqualTo("Tile");

        JsonNode fields = objectMapper.readTree(tile.fieldConfig());
        for (JsonNode node : fields.get("header")) {
            assertThat(MaterialSheetTemplateContext.HEADER_KEYS).contains(node.get("key").asText());
        }
        JsonNode measure = null;
        for (JsonNode node : fields.get("header")) {
            if ("measureAt".equals(node.get("key").asText())) measure = node;
        }
        assertThat(measure).isNotNull();
        assertThat(measure.get("visible").asBoolean()).isTrue();
        assertThat(measure.get("required").asBoolean()).isTrue();
        for (JsonNode node : fields.get("line")) {
            assertThat(MaterialSheetTemplateContext.LINE_KEYS).contains(node.get("key").asText());
        }

        JsonNode rows = objectMapper.readTree(tile.rowTemplateConfig());
        assertThat(rows).hasSize(8);
        assertThat(rows.get(0).asText()).isEqualTo("客厅地砖");
        assertThat(rows.get(7).asText()).isEqualTo("卫生间墙砖（湿）");

        JsonNode rules = objectMapper.readTree(tile.ruleConfig());
        rules.fieldNames().forEachRemaining(key ->
                assertThat(MaterialSheetTemplateContext.RULE_KEYS).contains(key));
        assertThat(rules.get("unitConversions").get("800*800").asDouble()).isEqualTo(0.64);
        assertThat(rules.get("unitConversions").get("750*1500").asDouble()).isEqualTo(1.125);
        assertThat(rules.get("realAreaIncludingReturn").asBoolean()).isTrue();
    }

    @Test
    void standardFieldsShouldPass() {
        String fields = """
                {"version":1,
                 "header":[{"key":"measureAt","label":"测量时间","visible":true,"required":true,"order":1}],
                 "line":[{"key":"locationName","label":"安装位置","visible":true,"required":true,"order":1},
                         {"key":"glass","label":"玻璃","visible":false,"required":false,"order":2}]}
                """;
        assertThat(context.validateFields(fields)).isEqualTo(fields.trim());
    }

    @Test
    void customFieldKeyMustBeRejected() {
        String fields = "{\"header\":[],\"line\":[{\"key\":\"myOwnField\",\"label\":\"企业自定义\"}]}";
        assertThatThrownBy(() -> context.validateFields(fields))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不在平台白名单内");
    }

    @Test
    void quantityAndPriceRulesMustBeRejected() {
        assertThatThrownBy(() -> context.validateRules("{\"lossRate\":0.08}"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("事实口径红线");
        assertThatThrownBy(() -> context.validateRules("{\"price\":100}"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("事实口径红线");
        assertThatThrownBy(() -> context.validateRules("{\"unknownKey\":1}"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不在平台白名单内");
    }

    @Test
    void signatureConfigMustBeWhitelisted() {
        assertThat(context.validateSignatures("{\"pm\":{\"required\":true}}")).contains("pm");
        assertThatThrownBy(() -> context.validateSignatures("{\"accountant\":{\"required\":true}}"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不在平台白名单内");
    }

    @Test
    void brokenJsonMustFailFast() {
        assertThatThrownBy(() -> context.validateFields("{not-json"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不是合法 JSON");
    }
}
