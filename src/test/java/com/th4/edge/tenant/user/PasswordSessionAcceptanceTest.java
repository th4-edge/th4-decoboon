package com.th4.edge.tenant.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** User 密码会话独立验收。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:password-acceptance;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.framework.security.password.bootstrap-token=acceptance-bootstrap",
        "th4.edge.framework.security.password.max-failed-attempts=3",
        "th4.edge.framework.security.password.lock-minutes=15",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
@Transactional
class PasswordSessionAcceptanceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private PasswordContext passwordContext;

    @Test
    void passwordSetupAndLoginMustWorkWithoutAnExistingToken() throws Exception {
        Employee employee = employee("13890000001");
        JsonNode setup = call("password-setup", """
                {"phone":"%s","bootstrapToken":"acceptance-bootstrap","password":"Pass1234","confirmPassword":"Pass1234"}
                """.formatted(employee.getTelNo()), null);

        assertThat(setup.path("success").asBoolean()).isTrue();
        assertThat(setup.path("data").path("token").asText()).isNotBlank();
        Credentials credential = credentialsRepository.findByTypeAndPhone(CredType.Password, employee.getTelNo());
        assertThat(credential.getPasswordHash()).startsWith("$2");
        assertThat(credential.getPasswordHash()).isNotEqualTo("Pass1234");
        assertThat(passwordContext.matches("Pass1234", credential.getPasswordHash())).isTrue();
        assertNoSecretFields(setup);

        JsonNode login = call("password-login", """
                {"phone":"%s","password":"Pass1234"}
                """.formatted(employee.getTelNo()), null);
        assertThat(login.path("success").asBoolean()).isTrue();
        assertThat(login.path("data").path("token").asText()).isNotBlank();
        assertNoSecretFields(login);
    }

    @Test
    void passwordLoginMustLockAfterRepeatedFailures() throws Exception {
        Employee employee = employee("13890000002");
        credential(employee, "Correct123");

        for (int attempt = 1; attempt <= 3; attempt++) {
            JsonNode failed = call("password-login", """
                    {"phone":"%s","password":"Wrong123"}
                    """.formatted(employee.getTelNo()), null);
            assertThat(failed.path("success").asBoolean()).isFalse();
        }
        Credentials locked = credentialsRepository.findByTypeAndPhone(CredType.Password, employee.getTelNo());
        assertThat(locked.getFailedAttempts()).isEqualTo(3);
        assertThat(locked.getLockedUntil()).isAfter(LocalDateTime.now());

        JsonNode correct = call("password-login", """
                {"phone":"%s","password":"Correct123"}
                """.formatted(employee.getTelNo()), null);
        assertThat(correct.path("success").asBoolean()).isFalse();
        assertThat(correct.path("errorCode").path("code").asText()).isEqualTo("ACCOUNT_LOCKED");
    }

    @Test
    void correctPasswordMustClearFailureCount() throws Exception {
        Employee employee = employee("13890000006");
        Credentials credential = credential(employee, "Correct123");

        for (int attempt = 1; attempt <= 2; attempt++) {
            JsonNode failed = call("password-login", """
                    {"phone":"%s","password":"Wrong123"}
                    """.formatted(employee.getTelNo()), null);
            assertThat(failed.path("success").asBoolean()).isFalse();
        }
        JsonNode success = call("password-login", """
                {"phone":"%s","password":"Correct123"}
                """.formatted(employee.getTelNo()), null);
        assertThat(success.path("success").asBoolean()).isTrue();
        Credentials cleared = credentialsRepository.findByCreId(credential.getCreId()).orElseThrow();
        assertThat(cleared.getFailedAttempts()).isZero();
        assertThat(cleared.getLockedUntil()).isNull();
    }

    @Test
    void disabledEmployeeMustNotLogin() throws Exception {
        Employee employee = employee("13890000003");
        employee.setStatus(EmployeeStatus.Suspended);
        employeeRepository.save(employee);
        credential(employee, "Correct123");

        JsonNode response = call("password-login", """
                {"phone":"%s","password":"Correct123"}
                """.formatted(employee.getTelNo()), null);
        assertThat(response.path("success").asBoolean()).isFalse();
        assertThat(response.path("errorCode").path("code").asText()).isEqualTo("403");
    }

    @Test
    void changeAndLogoutMustRequireTokenAndOldPassword() throws Exception {
        Employee employee = employee("13890000004");
        Credentials credential = credential(employee, "Correct123");
        JsonNode login = call("password-login", """
                {"phone":"%s","password":"Correct123"}
                """.formatted(employee.getTelNo()), null);
        String token = login.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        JsonNode change = call("password-change", """
                {"oldPassword":"Correct123","newPassword":"NewPass456","confirmPassword":"NewPass456"}
                """, token);
        assertThat(change.path("success").asBoolean()).isTrue();
        assertThat(passwordContext.matches("NewPass456",
                credentialsRepository.findByCreId(credential.getCreId()).orElseThrow().getPasswordHash())).isTrue();
        assertNoSecretFields(change);

        JsonNode logout = call("logout", "{}", token);
        assertThat(logout.path("success").asBoolean()).isTrue();

        JsonNode invalidated = call("info", "{}", token);
        assertThat(invalidated.path("success").asBoolean()).isFalse();
        assertThat(invalidated.path("errorCode").path("code").asText())
                .isIn("401", "tokenInvalid", "tokenExpired", "tokenIsUndefined");

        JsonNode afterLogout = call("password-login", """
                {"phone":"%s","password":"NewPass456"}
                """.formatted(employee.getTelNo()), null);
        assertThat(afterLogout.path("success").asBoolean()).isTrue();
    }

    @Test
    void changeAndLogoutMustRejectAnonymousRequests() throws Exception {
        JsonNode change = call("password-change", """
                {"oldPassword":"OldPass123","newPassword":"NewPass456","confirmPassword":"NewPass456"}
                """, null);
        assertThat(change.path("success").asBoolean()).isFalse();
        assertThat(change.path("errorCode").path("code").asText()).isEqualTo("tokenIsUndefined");

        JsonNode logout = call("logout", "{}", null);
        assertThat(logout.path("success").asBoolean()).isFalse();
        assertThat(logout.path("errorCode").path("code").asText()).isEqualTo("tokenIsUndefined");
    }

    @Test
    void weakPasswordMustBeRejected() throws Exception {
        Employee employee = employee("13890000005");
        JsonNode response = call("password-setup", """
                {"phone":"%s","bootstrapToken":"acceptance-bootstrap","password":"12345678","confirmPassword":"12345678"}
                """.formatted(employee.getTelNo()), null);
        assertThat(response.path("success").asBoolean()).isFalse();
        assertThat(response.path("errorCode").path("code").asText()).isEqualTo("PASSWORD_POLICY");
    }

    private JsonNode call(String action, String body, String token) throws Exception {
        var request = post("/api/auth/session/" + action)
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "acceptance-test")
                .content(body);
        if (token != null) request.header("token", token);
        MvcResult result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    private Employee employee(String phone) {
        Company company = new Company();
        company.setTitle("密码会话验收企业");
        company.setName("W" + Long.toHexString(System.nanoTime()));
        company.setSize(Size.A);
        company = companyRepository.save(company);
        Store store = new Store();
        store.setCompany(company);
        store.setName("S" + Long.toHexString(System.nanoTime()));
        store.setDefaultStore(true);
        store = storeRepository.save(store);
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(EmployeeRole.Boss);
        employee.setName("网页验收员工");
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

    private void assertNoSecretFields(JsonNode response) {
        String json = response.toString();
        assertThat(json).doesNotContain("passwordHash", "password_hash", "failedAttempts", "lockedUntil",
                "passwordUpdatedAt", "Pass1234", "NewPass456", "Correct123");
    }
}
