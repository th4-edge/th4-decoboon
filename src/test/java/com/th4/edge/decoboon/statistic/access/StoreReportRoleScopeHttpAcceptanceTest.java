package com.th4.edge.decoboon.statistic.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * `statistic/store/report`（门店经营概览）角色门禁与门店维度 HTTP 级验收
 * （`DESIGN-BIZ-20261013-003` P0D-02 / `DEC-067` BOSS-STORE-07 / `GAP-048`）。
 *
 * <ul>
 *   <li>门禁扩展为 BOSS / 总部财务 / 店长 / 门店财务；其他角色与未登录一律 403；</li>
 *   <li>店长与门店财务**锁定本店**（传他店 / 他企业门店 403）；</li>
 *   <li>门店维度经 provider 的 {@code @AutoInject} 字段绑定（请求模型不承载参数）；</li>
 *   <li>响应带 {@code storeScope}（scopeType/storeId/storeName/unassignedCount）；</li>
 *   <li>「全部门店」视图**不提供合计**：scopeType=AllStores 且不下发任何聚合数字；</li>
 *   <li>字段边界：响应不得出现成本 / 毛利 / 毛利率（`BR-003`）。</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:store-report-role-scope;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/store-report-role-scope-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
@Transactional
class StoreReportRoleScopeHttpAcceptanceTest {

    private static final String ROUTE = "statistic/store/report";
    private static final String PASSWORD = "Accept1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordContext passwordContext;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private EntityManager entityManager;

    private Long storeAId;
    private Long storeBId;
    private Long foreignStoreId;
    private String bossPhone;
    private String storeManagerPhone;
    private String hqFinancePhone;
    private String storeFinancePhone;
    private String designerPhone;

    @BeforeEach
    void setUp() {
        long seq = System.nanoTime();
        Company company = company("门店概览验收", seq);
        Store storeA = store(company, "概览总店", true);
        Store storeB = store(company, "概览城北店", false);
        Company other = company("门店概览他企", seq + 7);
        Store foreign = store(other, "概览他企门店", false);
        storeAId = storeA.getStoreId();
        storeBId = storeB.getStoreId();
        foreignStoreId = foreign.getStoreId();

        Employee boss = employee(company, storeA, EmployeeRole.Boss, "概览负责人", seq, 1);
        bossPhone = boss.getTelNo();
        storeManagerPhone = employee(company, storeA, EmployeeRole.StoreManager, "概览店长", seq, 2).getTelNo();
        // 总部财务：归属默认总店 → 企业范围（DEC-065）
        hqFinancePhone = employee(company, storeA, EmployeeRole.Finance, "概览总部财务", seq, 3).getTelNo();
        // 门店财务：归属非默认门店 → 门店范围
        storeFinancePhone = employee(company, storeB, EmployeeRole.Finance, "概览门店财务", seq, 4).getTelNo();
        designerPhone = employee(company, storeA, EmployeeRole.Designer, "概览设计师", seq, 5).getTelNo();

        // A 店 2 个客户/项目，B 店 1 个，另有 1 个未归属门店客户（不计入任何单店视图）
        customerWithProject(company, storeA, boss, seq, 11);
        customerWithProject(company, storeA, boss, seq, 12);
        customerWithProject(company, storeB, boss, seq, 13);
        unassignedCustomer(company, boss, seq, 14);
        entityManager.flush();
    }

    @Test
    void bossCanOpenStoreOverviewAndSeesChosenStore() throws Exception {
        String token = login(bossPhone);

        JsonNode storeA = api("{\"storeId\":" + storeAId + "}", token);
        assertThat(storeA.path("success").asBoolean())
                .as("BOSS 打开门店经营概览不再 403，实际：%s", storeA).isTrue();
        JsonNode dataA = storeA.path("data");
        assertThat(dataA.path("storeScope").path("scopeType").asText()).isEqualTo("Store");
        assertThat(dataA.path("storeScope").path("storeId").asLong()).isEqualTo(storeAId);
        assertThat(dataA.path("storeScope").path("storeName").asText()).isNotBlank();
        assertThat(dataA.path("storeName").asText()).isNotBlank();
        assertThat(dataA.path("customerCount").asInt()).as("A 店客户数按门店收窄").isEqualTo(2);
        assertThat(dataA.path("projectCount").asInt()).as("A 店项目数按门店收窄").isEqualTo(2);

        JsonNode storeB = api("{\"storeId\":" + storeBId + "}", token);
        assertThat(storeB.path("data").path("storeScope").path("storeId").asLong()).isEqualTo(storeBId);
        assertThat(storeB.path("data").path("customerCount").asInt()).isEqualTo(1);
    }

    @Test
    void allStoresViewGivesNoTotals() throws Exception {
        String token = login(bossPhone);

        JsonNode omitted = api("{}", token);
        assertThat(omitted.path("success").asBoolean()).as(omitted.toString()).isTrue();
        JsonNode scope = omitted.path("data").path("storeScope");
        assertThat(scope.path("scopeType").asText()).isEqualTo("AllStores");
        assertThat(scope.path("storeId").isMissingNode() || scope.path("storeId").isNull())
                .as("全部门店不落门店 ID").isTrue();
        assertNoAggregates(omitted.path("data"));

        JsonNode zero = api("{\"storeId\":0}", token);
        assertThat(zero.path("data").path("storeScope").path("scopeType").asText())
                .as("传 0 与不传等价（DEC-067 BOSS-STORE-10）").isEqualTo("AllStores");
        assertNoAggregates(zero.path("data"));
    }

    @Test
    void storeScopedRolesAreLockedToOwnStore() throws Exception {
        String storeManager = login(storeManagerPhone);
        JsonNode own = api("{}", storeManager);
        assertThat(own.path("success").asBoolean()).as(own.toString()).isTrue();
        assertThat(own.path("data").path("storeScope").path("scopeType").asText()).isEqualTo("Store");
        assertThat(own.path("data").path("storeScope").path("storeId").asLong()).isEqualTo(storeAId);
        assertThat(own.path("data").path("customerCount").asInt()).isEqualTo(2);
        assertForbidden(api("{\"storeId\":" + storeBId + "}", storeManager), "店长传他店");

        String storeFinance = login(storeFinancePhone);
        JsonNode financeOwn = api("{}", storeFinance);
        assertThat(financeOwn.path("success").asBoolean()).as(financeOwn.toString()).isTrue();
        assertThat(financeOwn.path("data").path("storeScope").path("storeId").asLong()).isEqualTo(storeBId);
        assertThat(financeOwn.path("data").path("customerCount").asInt()).isEqualTo(1);
        assertForbidden(api("{\"storeId\":" + storeAId + "}", storeFinance), "门店财务传他店");
    }

    @Test
    void hqFinanceCanNarrowByStoreButHasNoAllStoreTotals() throws Exception {
        String token = login(hqFinancePhone);
        JsonNode allStores = api("{}", token);
        assertThat(allStores.path("success").asBoolean()).as(allStores.toString()).isTrue();
        assertThat(allStores.path("data").path("storeScope").path("scopeType").asText()).isEqualTo("AllStores");
        assertNoAggregates(allStores.path("data"));

        JsonNode storeB = api("{\"storeId\":" + storeBId + "}", token);
        assertThat(storeB.path("success").asBoolean()).isTrue();
        assertThat(storeB.path("data").path("storeScope").path("storeId").asLong()).isEqualTo(storeBId);
        assertThat(storeB.path("data").path("customerCount").asInt()).isEqualTo(1);
    }

    @Test
    void foreignAndUnknownStoresAreRejected() throws Exception {
        String token = login(bossPhone);
        assertForbidden(api("{\"storeId\":" + foreignStoreId + "}", token), "他企业门店");
        assertForbidden(api("{\"storeId\":999999999999}", token), "不存在的门店");
    }

    @Test
    void otherRolesAreRejected() throws Exception {
        String designer = login(designerPhone);
        assertForbidden(api("{}", designer), "设计师");
        // 未登录由框架统一拦截（tokenIsUndefined），不得返回任何门店数据
        JsonNode anonymous = api("{}", null);
        assertThat(anonymous.path("success").asBoolean()).as("未登录不得拿到门店经营数据").isFalse();
        assertThat(anonymous.path("data").isMissingNode() || anonymous.path("data").isNull()).isTrue();
    }

    @Test
    void responseKeepsNoCostOrProfitFields() throws Exception {
        String token = login(bossPhone);
        JsonNode data = api("{\"storeId\":" + storeAId + "}", token).path("data");
        List<String> keys = new ArrayList<>();
        data.fieldNames().forEachRemaining(keys::add);
        for (String key : keys) {
            String lower = key.toLowerCase();
            assertThat(lower).as("门店经营概览不得新增成本 / 毛利字段（BR-003）：%s", key)
                    .doesNotContain("cost").doesNotContain("profit").doesNotContain("margin").doesNotContain("gross");
        }
    }

    // ---------- helpers ----------

    /** 「全部门店」视图不得下发任何聚合数字（比率与人员结构不可简单相加）。 */
    private void assertNoAggregates(JsonNode data) {
        for (String field : new String[]{"memberCount", "customerCount", "quoteCount", "projectCount",
                "designerCount", "marketerCount", "projectManagerCount", "taskTodoCount"}) {
            assertThat(data.path(field).asInt())
                    .as("全部门店视图不得给出合计 %s（实际 %s）", field, data.path(field)).isZero();
        }
        assertThat(data.path("designerPerformances").size()).as("全部门店视图不得给出人员结构排行").isZero();
        assertThat(data.path("recentCustomers").size()).isZero();
        assertThat(data.path("recentQuotes").size()).isZero();
        assertThat(data.path("recentProjects").size()).isZero();
    }

    private void assertForbidden(JsonNode response, String label) {
        assertThat(response.path("success").asBoolean()).as("%s 必须被拒绝：%s", label, response).isFalse();
        assertThat(response.path("errorCode").path("code").asText()).as(label).isEqualTo("403");
    }

    private JsonNode api(String body, String token) throws Exception {
        var request = post("/api/" + ROUTE)
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "store-report-role-scope-acceptance")
                .content(body);
        if (token != null) {
            request.header("token", token);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    private String login(String phone) throws Exception {
        JsonNode response = api0("auth/session/password-login",
                "{\"phone\":\"%s\",\"password\":\"%s\"}".formatted(phone, PASSWORD), null);
        assertThat(response.path("success").asBoolean()).as(response.toString()).isTrue();
        String token = response.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private JsonNode api0(String route, String body, String token) throws Exception {
        var request = post("/api/" + route)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            request.header("token", token);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    private Company company(String title, long seq) {
        Company company = new Company();
        company.setTitle(title + Math.abs(seq % 1000));
        company.setName("门店概览验收企业");
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

    private Employee employee(Company company, Store store, EmployeeRole role, String name, long seq, int offset) {
        String phone = "7" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L));
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
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

    private void unassignedCustomer(Company company, Employee operator, long seq, int offset) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setOwner(operator);
        customer.setName("概览未归属客户" + offset);
        customer.setTelNo("8" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        customer.setArea(new BigDecimal("100"));
        customer.setAddr("门店概览未归属地址");
        customerRepository.save(customer);
    }

    private void customerWithProject(Company company, Store store, Employee operator, long seq, int offset) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(operator);
        customer.setName("概览客户" + offset);
        customer.setTelNo("9" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        customer.setArea(new BigDecimal("120"));
        customer.setAddr("门店概览地址");
        customer.setStatus(CustStatus.Following);
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(operator);
        property.setCustomer(customer);
        property.setPropertyNo("PROP-SR-" + offset + "-" + Math.abs(seq % 100000));
        property.setAddress("门店概览小区 " + offset + " 栋 101");
        property.setArea(new BigDecimal("120"));
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("QUOTE-SR-" + offset + "-" + Math.abs(seq % 100000));
        quote.setTitle("门店概览报价");
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(operator);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(new BigDecimal("120"));
        quote.setSubtotal(new BigDecimal("100000"));
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setAmount(new BigDecimal("100000"));
        quote.setCost(new BigDecimal("80000"));
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(QuoteStatus.Accepted);
        quote = quoteRepository.save(quote);

        Project project = new Project();
        project.setNo("PROJECT-SR-" + offset + "-" + Math.abs(seq % 100000));
        project.setName("门店概览项目" + offset);
        project.setCompany(company);
        project.setStore(store);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setDesigner(operator);
        project.setManager(operator);
        project.setSupport(operator);
        project.setOwner(operator);
        project.setSiteAddr(property.getAddress());
        project.setArea(new BigDecimal("120"));
        project.setContractAmount(new BigDecimal("100000"));
        project.setCost(new BigDecimal("80000"));
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(10);
        project.setStartDate(LocalDate.now());
        project.setPlanEndDate(LocalDate.now().plusDays(90));
        projectRepository.save(project);
    }
}
