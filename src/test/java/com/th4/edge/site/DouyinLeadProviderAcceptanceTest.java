package com.th4.edge.site;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.site.configure.SiteLeadStatus;
import com.th4.edge.site.model.SiteLead;
import com.th4.edge.site.repository.SiteLeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 抖音渠道公开留资入口独立验收（L1 自营获客 · 装得清经营体检）。
 *
 * <p>验证线索进入平台域 SiteLead、source=douyin、归因与蜜罐逻辑正确，
 * 且不触碰任何租户数据。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:douyin-lead;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.site-dir=src/test/resources/site",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
class DouyinLeadProviderAcceptanceTest {

    private static final String ROUTE = "/api/site/lead/douyin/submit";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SiteLeadRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void shouldPersistDouyinLeadWithAttribution() throws Exception {
        Map<String, Object> request = baseRequest();
        request.put("painPoint", "成本利润不清");
        request.put("enterpriseType", "单店成长型");
        request.put("sourceScene", "entry-3");
        request.put("contentId", "C-1001");
        request.put("videoId", "V-2002");
        request.put("anchorId", "A-3003");

        MvcResult result = mockMvc.perform(post(ROUTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("leadId").asLong()).isPositive();
        assertThat(body.path("data").path("status").asText()).isEqualTo("New");
        // 响应不得回显手机号
        assertThat(body.toString()).doesNotContain("13800000000");

        List<SiteLead> leads = repository.findAll();
        assertThat(leads).hasSize(1);
        SiteLead lead = leads.get(0);
        assertThat(lead.getName()).isEqualTo("王总");
        assertThat(lead.getContact()).isEqualTo("13800000000");
        assertThat(lead.getTopic()).isEqualTo("成本利润不清");
        assertThat(lead.getSource()).isEqualTo("douyin");
        assertThat(lead.getStatus()).isEqualTo(SiteLeadStatus.New);
        assertThat(lead.getConsent()).isTrue();
        assertThat(lead.getNote()).contains("企业类型：单店成长型").contains("来源场景：entry-3");
        assertThat(lead.getPageUrl()).isEqualTo("douyin://c=C-1001&v=V-2002&a=A-3003");
        assertThat(lead.getCreateTime()).isNotNull();
    }

    @Test
    void shouldRejectHoneypot() throws Exception {
        Map<String, Object> request = baseRequest();
        request.put("website", "https://spam.example");

        JsonNode body = response(request);
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectMissingConsent() throws Exception {
        Map<String, Object> request = baseRequest();
        request.put("consent", false);

        JsonNode body = response(request);
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectMissingPainPoint() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", "王总");
        request.put("contact", "13800000000");
        request.put("consent", true);

        JsonNode body = response(request);
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(repository.findAll()).isEmpty();
    }

    private JsonNode response(Map<String, Object> request) throws Exception {
        MvcResult result = mockMvc.perform(post(ROUTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> baseRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", "王总");
        request.put("contact", "13800000000");
        request.put("painPoint", "报价漏项");
        request.put("consent", true);
        return request;
    }
}
