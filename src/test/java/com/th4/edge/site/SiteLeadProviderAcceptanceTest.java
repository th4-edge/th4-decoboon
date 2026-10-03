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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 官网公开留资入口独立验收。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:site-lead;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class SiteLeadProviderAcceptanceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SiteLeadRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void shouldPersistAnonymousWebsiteLead() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", "张总");
        request.put("contact", "13800000000");
        request.put("topic", "材料采购和欠款");
        request.put("note", "月底对账总是对不上");
        request.put("pageUrl", "https://decoboon.joytsing.com/site/");
        request.put("website", "");
        request.put("consent", true);

        MvcResult result = mockMvc.perform(post("/api/site/lead/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("leadId").asLong()).isPositive();
        assertThat(body.path("data").path("status").asText()).isEqualTo("New");
        assertThat(body.toString()).doesNotContain("13800000000");

        List<SiteLead> leads = repository.findAll();
        assertThat(leads).hasSize(1);
        SiteLead lead = leads.get(0);
        assertThat(lead.getName()).isEqualTo("张总");
        assertThat(lead.getContact()).isEqualTo("13800000000");
        assertThat(lead.getTopic()).isEqualTo("材料采购和欠款");
        assertThat(lead.getNote()).isEqualTo("月底对账总是对不上");
        assertThat(lead.getPageUrl()).isEqualTo("https://decoboon.joytsing.com/site/");
        assertThat(lead.getSource()).isEqualTo("website");
        assertThat(lead.getStatus()).isEqualTo(SiteLeadStatus.New);
        assertThat(lead.getConsent()).isTrue();
        assertThat(lead.getCreateTime()).isNotNull();
    }

    @Test
    void shouldServeSiteBundleAtSiteEntry() throws Exception {
        mockMvc.perform(get("/site/"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Type", "text/html;charset=UTF-8"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Language", "zh-CN"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        org.hamcrest.Matchers.containsString("site-preview")));

        mockMvc.perform(get("/site/index.html"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Type", "text/html;charset=UTF-8"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        org.hamcrest.Matchers.containsString("site-preview")));

        mockMvc.perform(get("/site/site.webmanifest"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Type", "application/manifest+json;charset=UTF-8"));
    }

    @Test
    void shouldRejectHoneypotAndMissingConsent() throws Exception {
        Map<String, Object> honeypot = new LinkedHashMap<>();
        honeypot.put("name", "机器人");
        honeypot.put("contact", "000");
        honeypot.put("topic", "测试");
        honeypot.put("website", "https://spam.example");
        honeypot.put("consent", true);

        JsonNode honeypotBody = response(honeypot);
        assertThat(honeypotBody.path("success").asBoolean()).isFalse();
        assertThat(repository.count()).isZero();

        Map<String, Object> missingConsent = new LinkedHashMap<>();
        missingConsent.put("name", "张总");
        missingConsent.put("contact", "13800000000");
        missingConsent.put("topic", "客户跟到哪了");
        missingConsent.put("website", "");
        missingConsent.put("consent", false);

        JsonNode consentBody = response(missingConsent);
        assertThat(consentBody.path("success").asBoolean()).isFalse();
        assertThat(repository.count()).isZero();
    }

    private JsonNode response(Map<String, Object> request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/site/lead/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
