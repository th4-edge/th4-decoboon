package com.th4.edge.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.configure.UserStatus;
import com.th4.edge.tenant.user.context.PasswordContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 平台运营管理端权限、订阅调整、审计和监控独立验收。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:platform-admin;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "decoboon.platform.bootstrap.enabled=true",
        "decoboon.platform.bootstrap.company-title=装得清平台运营中心",
        "decoboon.platform.bootstrap.company-name=平台运营",
        "decoboon.platform.bootstrap.phone=13900000001",
        "decoboon.platform.bootstrap.password=AdminPass123",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
class PlatformAdminAcceptanceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private PasswordContext passwordContext;
    @Autowired private SubscriptionContext subscriptionContext;

    @Test
    void platformAdminMustQueryAllTenantsAdjustSubscriptionAndLeaveAudit() throws Exception {
        Employee tenantBoss = employee(false, "13900000002", "普通企业负责人");
        credential(tenantBoss, "TenantPass123");
        Company tenant = tenantBoss.getCompany();
        Subscription subscription = subscriptionContext.grantTrial(tenant);

        String adminToken = login("13900000001", "AdminPass123");
        JsonNode tenants = call("platform/tenant/page", """
                {"current":1,"size":20,"keyword":"普通企业"}
                """, adminToken);
        assertThat(tenants.path("success").asBoolean()).isTrue();
        assertThat(tenants.path("data").path("content").size()).isEqualTo(1);
        assertThat(tenants.path("data").path("content").get(0).path("title").asText()).isEqualTo("普通企业");

        JsonNode adjusted = call("platform/subscription/update", """
                {"subId":%d,"days":30,"extraSeats":2,"extraStores":1,"reason":"客户成功人工续费并扩容"}
                """.formatted(subscription.getSubId()), adminToken);
        assertThat(adjusted.path("success").asBoolean()).isTrue();
        assertThat(adjusted.path("data").path("remainingDays").asInt()).isGreaterThan(30);
        assertThat(adjusted.path("data").path("extraSeats").asInt()).isEqualTo(2);
        assertThat(adjusted.path("data").path("extraStores").asInt()).isEqualTo(1);

        JsonNode audit = call("platform/operation/page", """
                {"current":1,"size":20,"companyId":%d}
                """.formatted(tenant.getComId()), adminToken);
        assertThat(audit.path("success").asBoolean()).isTrue();
        assertThat(audit.path("data").path("content").size()).isEqualTo(1);
        assertThat(audit.path("data").path("content").get(0).path("reason").asText())
                .isEqualTo("客户成功人工续费并扩容");

        JsonNode dashboard = call("platform/dashboard/info", "{}", adminToken);
        assertThat(dashboard.path("success").asBoolean()).isTrue();
        assertThat(dashboard.path("data").path("totalCompanies").asLong()).isGreaterThanOrEqualTo(1);

        JsonNode system = call("platform/system/status", "{}", adminToken);
        assertThat(system.path("success").asBoolean()).isTrue();
        assertThat(system.path("data").path("services").size()).isGreaterThanOrEqualTo(3);

        String tenantToken = login("13900000002", "TenantPass123");
        JsonNode forbidden = call("platform/tenant/page", "{}", tenantToken);
        assertThat(forbidden.path("success").asBoolean()).isFalse();
        assertThat(forbidden.path("errorCode").path("code").asText()).isEqualTo("403");

        JsonNode anonymous = call("platform/tenant/page", "{}", null);
        assertThat(anonymous.path("success").asBoolean()).isFalse();
        assertThat(anonymous.path("errorCode").path("code").asText())
                .isIn("401", "tokenIsUndefined", "tokenInvalid", "tokenExpired");
    }

    private String login(String phone, String password) throws Exception {
        JsonNode response = call("auth/session/password-login", """
                {"phone":"%s","password":"%s"}
                """.formatted(phone, password), null);
        assertThat(response.path("success").asBoolean()).isTrue();
        return response.path("data").path("token").asText();
    }

    private JsonNode call(String route, String body, String token) throws Exception {
        var request = post("/api/" + route)
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "platform-acceptance")
                .content(body);
        if (token != null) request.header("token", token);
        MvcResult result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank()
                ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    private Employee employee(boolean platform, String phone, String name) {
        Company company = new Company();
        company.setTitle(platform ? "装得清平台运营中心" : "普通企业");
        company.setName(platform ? "平台运营" : "普通企业");
        company.setSize(platform ? Size.A : Size.B);
        company.setPlatform(platform);
        company = companyRepository.save(company);
        Store store = new Store();
        store.setCompany(company);
        store.setName(platform ? "平台总店" : "企业总店");
        store.setDefaultStore(true);
        store = storeRepository.save(store);
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(EmployeeRole.Boss);
        employee.setName(name);
        employee.setTelNo(phone);
        employee.setStatus(EmployeeStatus.Active);
        employee.setJoinedAt(LocalDateTime.now());
        return employeeRepository.save(employee);
    }

    private Credentials credential(Employee employee, String rawPassword) {
        Credentials credential = new Credentials();
        credential.setType(CredType.Password);
        credential.setIdKey(employee.getTelNo());
        credential.setIdSecret("password");
        credential.setPhone(employee.getTelNo());
        credential.setStatus(UserStatus.Active);
        credential.setEmployee(employee);
        credential.setPasswordHash(passwordContext.encode(rawPassword));
        credential.setPasswordUpdatedAt(LocalDateTime.now());
        credential.setFailedAttempts(0);
        credential.setMustChangePassword(false);
        return credentialsRepository.save(credential);
    }
}
