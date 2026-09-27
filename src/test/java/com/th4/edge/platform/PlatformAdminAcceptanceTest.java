package com.th4.edge.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.platform.model.PlatformRole;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.repository.PlatformOrgRepository;
import com.th4.edge.platform.repository.PlatformUserRepository;
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
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 平台独立组织、认证、租户适配、订阅调整、审计和监控验收。 */
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
        "decoboon.platform.bootstrap.org-code=DECOBOON",
        "decoboon.platform.bootstrap.org-name=装得清平台运营中心",
        "decoboon.platform.bootstrap.department-code=PLATFORM",
        "decoboon.platform.bootstrap.department-name=平台运营中心",
        "decoboon.platform.bootstrap.phone=13900000001",
        "decoboon.platform.bootstrap.username=平台超级管理员",
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
    @Autowired private PlatformUserRepository platformUserRepository;
    @Autowired private PlatformOrgRepository platformOrgRepository;
    @Autowired private DataSource dataSource;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void platformIdentityMustStayIsolatedAndOperateTenantThroughScalarAdapter() throws Exception {
        Employee tenantBoss = employee("13900000002", "普通企业负责人");
        credential(tenantBoss, "TenantPass123");
        Company tenant = tenantBoss.getCompany();
        Subscription subscription = subscriptionContext.grantTrial(tenant);

        assertThat(platformUserRepository.findByPhone("13900000001").getRole()).isEqualTo(PlatformRole.SuperAdmin);
        assertThat(platformOrgRepository.findByCode("DECOBOON")).isNotNull();
        assertThat(employeeRepository.findByTelNo("13900000001")).isNull();

        String adminToken = platformLogin("13900000001", "AdminPass123");
        JsonNode platformIdentity = call("platform/auth/info", "{}", adminToken);
        assertThat(platformIdentity.path("success").asBoolean()).isTrue();
        assertThat(platformIdentity.path("data").path("role").path("value").asText()).isEqualTo("SuperAdmin");
        assertThat(platformIdentity.path("data").path("org").path("code").asText()).isEqualTo("DECOBOON");
        assertThat(platformIdentity.toString()).doesNotContain("passwordHash", "password_hash");

        JsonNode tenants = call("platform/tenant/page", """
                {"current":1,"size":20,"keyword":"普通企业"}
                """, adminToken);
        assertThat(tenants.path("success").asBoolean()).isTrue();
        assertThat(tenants.path("data").path("content").size()).isEqualTo(1);
        assertThat(tenants.path("data").path("content").get(0).path("title").asText()).isEqualTo("普通企业");
        assertThat(tenants.path("data").path("content").get(0).has("platform")).isFalse();
        assertThat(tenants.path("data").path("content").get(0).path("subId").asLong()).isEqualTo(subscription.getSubId());

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
        JsonNode log = audit.path("data").path("content").get(0);
        assertThat(log.path("targetComId").asLong()).isEqualTo(tenant.getComId());
        assertThat(log.path("targetCompanyTitle").asText()).isEqualTo("普通企业");
        assertThat(log.path("operator").path("userId").asLong()).isEqualTo(platformUserRepository.findByPhone("13900000001").getUserId());
        assertThat(log.has("company")).isFalse();

        JsonNode dashboard = call("platform/dashboard/info", "{}", adminToken);
        assertThat(dashboard.path("success").asBoolean()).isTrue();
        assertThat(dashboard.path("data").path("totalCompanies").asLong()).isGreaterThanOrEqualTo(1);

        JsonNode system = call("platform/system/status", "{}", adminToken);
        assertThat(system.path("success").asBoolean()).isTrue();
        assertThat(system.path("data").path("services").size()).isGreaterThanOrEqualTo(3);

        String tenantToken = tenantLogin("13900000002", "TenantPass123");
        JsonNode forbidden = call("platform/tenant/page", "{}", tenantToken);
        assertThat(forbidden.path("success").asBoolean()).isFalse();
        assertThat(forbidden.path("errorCode").path("code").asText()).isIn("401", "403", "userOfTokenNotMatch");

        JsonNode anonymous = call("platform/tenant/page", "{}", null);
        assertThat(anonymous.path("success").asBoolean()).isFalse();
        assertThat(anonymous.path("errorCode").path("code").asText())
                .isIn("401", "tokenIsUndefined", "tokenInvalid", "tokenExpired");

        JsonNode logout = call("platform/auth/logout", "{}", adminToken);
        assertThat(logout.path("success").asBoolean()).isTrue();
        JsonNode afterLogout = call("platform/auth/info", "{}", adminToken);
        assertThat(afterLogout.path("success").asBoolean()).isFalse();
    }

    @Test
    void onboardingLedgerMustExposeOwnerContactAndServiceState() throws Exception {
        Employee boss = employee("13900000003", "入驻台账负责人");
        Company company = boss.getCompany();
        company.setTitle("入驻台账测试企业");
        company.setName("入驻台账测试企业");
        company = companyRepository.save(company);
        boss.setCompany(company);
        boss = employeeRepository.save(boss);
        Credentials bossCredential = credential(boss, "TenantPass123");
        bossCredential.setLastTime(LocalDateTime.now().minusMinutes(5));
        credentialsRepository.save(bossCredential);

        String adminToken = platformLogin("13900000001", "AdminPass123");
        JsonNode page = call("platform/tenant/onboarding/page", """
                {"keyword":"入驻台账测试企业","current":1,"size":20}
                """, adminToken);
        assertThat(page.path("success").asBoolean()).isTrue();
        JsonNode item = page.path("data").path("content").get(0);
        assertThat(item.path("companyTitle").asText()).isEqualTo("入驻台账测试企业");
        assertThat(item.path("ownerName").asText()).isEqualTo("入驻台账负责人");
        assertThat(item.path("ownerPhoneMasked").asText()).isEqualTo("139****0003");
        assertThat(item.path("lastLoginAt").asText()).isNotBlank();
        assertThat(item.path("ownerCount").asInt()).isEqualTo(1);
        assertThat(page.toString()).doesNotContain("13900000003");

        JsonNode reveal = call("platform/tenant/onboarding/reveal", """
                {"companyId":%d,"empId":%d,"reason":"首次入驻电话服务"}
                """.formatted(company.getComId(), boss.getEmpId()), adminToken);
        assertThat(reveal.path("success").asBoolean()).isTrue();
        assertThat(reveal.path("data").path("phone").asText()).isEqualTo("13900000003");

        JsonNode logs = call("platform/operation/page", """
                {"companyId":%d,"action":"OwnerPhoneReveal","current":1,"size":20}
                """.formatted(company.getComId()), adminToken);
        assertThat(logs.path("data").path("content").size()).isGreaterThan(0);

        JsonNode saved = call("platform/tenant/onboarding/service/save", """
                {"companyId":%d,"status":"Contacted","contactResult":"已电话确认负责人","nextAction":"引导建立第一个客户","nextFollowAt":"2026-10-01T10:00:00","remark":"负责人可正常联系","reason":"首次入驻服务"}
                """.formatted(company.getComId()), adminToken);
        assertThat(saved.path("success").asBoolean()).isTrue();

        JsonNode detail = call("platform/tenant/onboarding/info", """
                {"companyId":%d}
                """.formatted(company.getComId()), adminToken);
        assertThat(detail.path("success").asBoolean()).isTrue();
        assertThat(detail.path("data").path("serviceStatus").path("key").asText()).isEqualTo("Contacted");
        assertThat(detail.path("data").path("serviceRecords").size()).isEqualTo(1);
        assertThat(detail.path("data").path("owners").get(0).path("phoneMasked").asText()).isEqualTo("139****0003");

        Employee tenant = employee("13900000004", "普通验收用户");
        Company tenantCompany = tenant.getCompany();
        tenantCompany.setTitle("越权验收企业");
        tenantCompany.setName("越权验收企业");
        tenantCompany = companyRepository.save(tenantCompany);
        tenant.setCompany(tenantCompany);
        tenant = employeeRepository.save(tenant);
        credential(tenant, "TenantPass123");
        String tenantToken = tenantLogin("13900000004", "TenantPass123");
        JsonNode forbidden = call("platform/tenant/onboarding/page", "{}", tenantToken);
        assertThat(forbidden.path("success").asBoolean()).isFalse();
    }

    @Test
    void platformTablesMustNotReferenceTenantTables() throws Exception {
        Set<String> referencedTables = new LinkedHashSet<>();
        try (Connection connection = dataSource.getConnection()) {
            for (String table : new String[]{"db_platform_org", "db_platform_department", "db_platform_user", "db_platform_operation_log", "db_platform_onboarding_service"}) {
                try (ResultSet keys = connection.getMetaData().getImportedKeys(null, null, table)) {
                    while (keys.next()) referencedTables.add(keys.getString("PKTABLE_NAME").toLowerCase());
                }
            }
        }
        assertThat(referencedTables).allMatch(table -> table.startsWith("db_platform_"));
        assertThat(referencedTables).doesNotContain("db_tenant_company", "db_tenant_employee", "db_tenant_credentials");
        assertThat(entityManagerFactory.getMetamodel().entity(Company.class).getAttributes())
                .extracting(attribute -> attribute.getName()).doesNotContain("platform");
    }

    private String platformLogin(String phone, String password) throws Exception {
        JsonNode response = call("platform/auth/login", """
                {"phone":"%s","password":"%s"}
                """.formatted(phone, password), null);
        assertThat(response.path("success").asBoolean()).isTrue();
        return response.path("data").path("token").asText();
    }

    private String tenantLogin(String phone, String password) throws Exception {
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

    private Employee employee(String phone, String name) {
        Company company = new Company();
        company.setTitle("普通企业");
        company.setName("普通企业");
        company.setSize(Size.B);
        company = companyRepository.save(company);
        Store store = new Store();
        store.setCompany(company);
        store.setName("企业总店");
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
