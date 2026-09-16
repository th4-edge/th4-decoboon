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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 生产回归：会话信息接口必须在关闭 Open Session in View 后正常序列化员工归属。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-session-lazy;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.framework.security.password.bootstrap-token=acceptance-bootstrap",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
class AuthSessionInfoLazyAcceptanceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private PasswordContext passwordContext;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void sessionInfoMustSerializeEmployeeGraphAfterRepositoryTransactionEnds() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long empId = tx.execute(status -> {
            Company company = new Company();
            company.setTitle("懒加载回归企业");
            company.setName("LAZY" + System.nanoTime());
            company.setSize(Size.A);
            company = companyRepository.save(company);

            Store store = new Store();
            store.setCompany(company);
            store.setName("默认门店");
            store.setDefaultStore(true);
            store = storeRepository.save(store);

            Employee employee = new Employee();
            employee.setCompany(company);
            employee.setStore(store);
            employee.setRole(EmployeeRole.Boss);
            employee.setName("懒加载回归员工");
            employee.setTelNo("13890000007");
            employee.setStatus(EmployeeStatus.Active);
            employee.setJoinedAt(LocalDateTime.now());
            employee = employeeRepository.save(employee);

            Credentials credential = new Credentials();
            credential.setType(CredType.Password);
            credential.setIdKey(employee.getTelNo());
            credential.setIdSecret("password");
            credential.setPhone(employee.getTelNo());
            credential.setStatus(UserStatus.Active);
            credential.setEmployee(employee);
            credential.setPasswordHash(passwordContext.encode("Pass1234"));
            credential.setPasswordUpdatedAt(LocalDateTime.now());
            credential.setFailedAttempts(0);
            credential.setMustChangePassword(false);
            credentialsRepository.save(credential);
            return employee.getEmpId();
        });

        JsonNode login = postJson("/api/auth/session/password-login",
                "{\"phone\":\"13890000007\",\"password\":\"Pass1234\"}", null);
        assertThat(login.path("success").asBoolean()).isTrue();
        String token = login.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        // 模拟生产请求边界：登录后的 fixture 事务已经提交，info Provider 必须能序列化完整员工图。
        JsonNode info = postJson("/api/auth/session/info", "{}", token);
        assertThat(info.path("success").asBoolean()).isTrue();
        assertThat(info.path("data").path("employee").path("empId").asLong()).isEqualTo(empId);
        assertThat(info.path("data").path("employee").path("company").path("comId").asLong()).isPositive();
        assertThat(info.path("data").path("employee").path("store").path("storeId").asLong()).isPositive();
    }

    private JsonNode postJson(String uri, String body, String token) throws Exception {
        var request = post(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) request.header("token", token);
        MvcResult result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }
}
