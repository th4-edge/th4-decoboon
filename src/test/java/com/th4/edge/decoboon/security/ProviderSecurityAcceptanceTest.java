package com.th4.edge.decoboon.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.common.configure.ProviderSecurityValidator;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PP-07 Provider 安全和公开白名单独立验收。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:provider-security;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
class ProviderSecurityAcceptanceTest {

    private static final Set<String> EXPECTED_PUBLIC_ROUTES = Set.of(
            "auth/session/login",
            "auth/session/password-login",
            "auth/session/password-setup",
            "auth/session/register",
            "identity/customer-invite/accept",
            "identity/customer-invite/info",
            "invite/invitation/accept",
            "invite/invitation/info",
            "invite/invitation/reject",
            "order/callback",
            "file/private/download"
    );

    @Autowired private ApplicationContext applicationContext;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldInventoryEveryProviderWithExplicitSecurityOrPublicWhitelist() {
        Map<String, Class<?>> routes = new LinkedHashMap<>();
        for (Provider<?, ?> provider : applicationContext.getBeansOfType(Provider.class).values()) {
            Class<?> target = AopUtils.getTargetClass(provider);
            if (!target.getName().startsWith("com.th4.edge.decoboon")
                    && !target.getName().startsWith("com.th4.edge.tenant")
                    && !target.getName().startsWith("com.th4.edge.platform")) {
                continue;
            }
            Service service = AnnotationUtils.findAnnotation(target, Service.class);
            if (service == null || service.value().isBlank() || service.value().contains("{")) {
                continue;
            }
            assertThat(routes.put(service.value(), target))
                    .as("duplicate Provider route: " + service.value())
                    .isNull();
            assertThat(EXPECTED_PUBLIC_ROUTES.contains(service.value()) || provider instanceof Security)
                    .as("route without Security or whitelist: " + service.value())
                    .isTrue();
        }

        assertThat(ProviderSecurityValidator.PUBLIC_ROUTES).containsExactlyInAnyOrderElementsOf(EXPECTED_PUBLIC_ROUTES);
        assertThat(routes.keySet()).containsAll(EXPECTED_PUBLIC_ROUTES);
        // 2026-09-16 同步：新增平台运营 7 条受保护 Provider 后路由总数为 205（public 11 + protected 194）
        assertThat(routes).hasSize(205);
        assertThat(routes.keySet()).filteredOn(route -> !EXPECTED_PUBLIC_ROUTES.contains(route)).hasSize(194);
        assertThat(routes.keySet()).filteredOn(EXPECTED_PUBLIC_ROUTES::contains).hasSize(11);
    }

    @Test
    void protectedRepresentativeRoutesMustReturnUnauthorizedWithoutToken() throws Exception {
        assertUnauthorized("project/project/page");
        assertUnauthorized("project/task/update");
        assertUnauthorized("identity/customer-invite/save");
        assertUnauthorized("file/private/grant");
        assertUnauthorized("finance/receivable/page");
    }

    @Test
    void publicRepresentativeContractsMustNotBeBlockedByLoginSecurity() throws Exception {
        assertNotUnauthorized("auth/session/login", "{}");
        assertNotUnauthorized("auth/session/password-login", "{}");
        assertNotUnauthorized("auth/session/password-setup", "{}");
        assertNotUnauthorized("identity/customer-invite/info", "{}");
        assertNotUnauthorized("invite/invitation/info", "{}");
        assertNotUnauthorized("order/callback", "{}");
        assertNotUnauthorized("file/private/download", "{}");
    }

    private void assertUnauthorized(String route) throws Exception {
        JsonNode body = post(route, "{}");
        int status = lastStatus;
        String code = body.path("errorCode").path("code").asText();
        assertThat(status == 401 || Set.of("401", "tokenIsUndefined", "tokenInvalid", "tokenExpired").contains(code))
                .as("route should require authentication: " + route + ", status=" + status + ", code=" + code)
                .isTrue();
    }

    private void assertNotUnauthorized(String route, String body) throws Exception {
        JsonNode response = post(route, body);
        assertThat(lastStatus).as("public route must not return HTTP 401: " + route).isNotEqualTo(401);
        assertThat(response.path("errorCode").path("code").asText())
                .as("public route must not return business 401: " + route)
                .isNotEqualTo("401");
    }

    private int lastStatus;

    private JsonNode post(String route, String body) throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/" + route)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "acceptance-test")
                        .content(body))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }
}
