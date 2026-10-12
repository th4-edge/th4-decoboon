package com.th4.edge.decoboon.customer.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 客户分页/客户经营聚合的**门店维度范围标注**（P1 任务 3，`DEC-067` BOSS-STORE-03）。
 *
 * <p>「含未归属门店 N 条」必须是**全量口径**（服务端 SQL 计数），不得用当前页 200 行的局部计数冒充；
 * 并断言恒等式：<b>全部门店合计 = Σ各门店 + 未归属门店</b>。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:customer-store-scope;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/customer-store-scope-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
@Transactional
class CustomerStoreScopeAnnotationTest {

    private static final String PASSWORD = "Accept1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordContext passwordContext;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private EntityManager entityManager;

    private Long storeAId;
    private Long storeBId;
    private String bossPhone;

    @BeforeEach
    void setUp() {
        long seq = System.nanoTime();
        Company company = company("客户范围标注", seq);
        Store storeA = store(company, "标注总店", true);
        Store storeB = store(company, "标注分店", false);
        storeAId = storeA.getStoreId();
        storeBId = storeB.getStoreId();
        Employee boss = employee(company, storeA, "标注负责人", seq, 1);
        bossPhone = boss.getTelNo();

        customer(company, storeA, boss, "标注客户A", seq, 11);
        customer(company, storeB, boss, "标注客户B", seq, 12);
        customer(company, null, boss, "标注未归属客户", seq, 13);
        entityManager.flush();
    }

    @Test
    void unassignedCountIsFullScopeAndIdentityHolds() throws Exception {
        String token = login(bossPhone);

        JsonNode allOverview = api("customer/customer/overview", "{}", token);
        JsonNode allScope = allOverview.path("data").path("storeScope");
        assertThat(allScope.path("scopeType").asText()).isEqualTo("AllStores");
        assertThat(allScope.path("unassignedCount").asInt()).as("未归属门店客户全量条数（服务端 SQL 计数）").isEqualTo(1);

        JsonNode storeAOverview = api("customer/customer/overview", "{\"storeId\":" + storeAId + "}", token);
        JsonNode storeAScope = storeAOverview.path("data").path("storeScope");
        assertThat(storeAScope.path("scopeType").asText()).isEqualTo("Store");
        assertThat(storeAScope.path("storeId").asLong()).isEqualTo(storeAId);
        assertThat(storeAScope.path("storeName").asText()).isNotBlank();
        assertThat(storeAScope.path("unassignedCount").asInt()).as("单店视图不含未归属").isZero();

        long totalAll = totalOf(api("customer/customer/page", "{\"size\":200}", token));
        long totalA = totalOf(api("customer/customer/page", "{\"size\":200,\"storeId\":" + storeAId + "}", token));
        long totalB = totalOf(api("customer/customer/page", "{\"size\":200,\"storeId\":" + storeBId + "}", token));

        assertThat(totalA).isEqualTo(1);
        assertThat(totalB).isEqualTo(1);
        assertThat(totalAll)
                .as("恒等式：全部门店 = Σ各门店 + 未归属（%s = %s + %s + 1）", totalAll, totalA, totalB)
                .isEqualTo(totalA + totalB + allScope.path("unassignedCount").asInt());
    }

    // ---------- 工具 ----------

    private long totalOf(JsonNode response) {
        JsonNode data = response.path("data");
        JsonNode total = data.path("totalElements");
        if (total.isMissingNode()) {
            total = data.path("total");
        }
        return total.asLong();
    }

    private JsonNode api(String route, String body, String token) throws Exception {
        var request = post("/api/" + route).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request.header("token", token);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    private String login(String phone) throws Exception {
        JsonNode response = api("auth/session/password-login",
                "{\"phone\":\"%s\",\"password\":\"%s\"}".formatted(phone, PASSWORD), null);
        return response.path("data").path("token").asText();
    }

    private Company company(String title, long seq) {
        Company company = new Company();
        company.setTitle(title + Math.abs(seq % 1000));
        company.setName("客户范围标注企业");
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company, String name, boolean defaultStore) {
        Store store = new Store();
        store.setCompany(company);
        store.setName(name);
        store.setDefaultStore(defaultStore);
        return storeRepository.save(store);
    }

    private Employee employee(Company company, Store store, String name, long seq, int offset) {
        String phone = "6" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L));
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(EmployeeRole.Boss);
        employee.setStatus(EmployeeStatus.Active);
        employee.setName(name);
        employee.setTelNo(phone);
        employee.setJoinedAt(LocalDateTime.now());
        employee = employeeRepository.save(employee);

        Credentials credential = new Credentials();
        credential.setType(CredType.Password);
        credential.setIdKey(phone);
        credential.setIdSecret("password");
        credential.setPhone(phone);
        credential.setStatus(UserStatus.Active);
        credential.setEmployee(employee);
        credential.setPasswordHash(passwordContext.encode(PASSWORD));
        credential.setPasswordUpdatedAt(LocalDateTime.now());
        credential.setFailedAttempts(0);
        credential.setMustChangePassword(false);
        credentialsRepository.save(credential);
        return employee;
    }

    private void customer(Company company, Store store, Employee owner, String name, long seq, int offset) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(owner);
        customer.setName(name);
        customer.setStatus(CustStatus.New);
        customer.setTelNo("7" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        customer.setArea(new BigDecimal("100"));
        customerRepository.save(customer);
    }
}
