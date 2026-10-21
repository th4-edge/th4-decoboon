package com.th4.edge.decoboon.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * DEC-065 财务门店口径 HTTP 级独立验收：真实登录态 + 真实 Provider 路由 + 真实序列化。
 *
 * <p>覆盖：门店财务/店长只读本店、总部财务与 BOSS 全门店并可下钻、跨店与跨企业越权拒绝、
 * 店长记账写动作成功且归属本店、无关角色被拒。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-store-scope-http;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/finance-store-scope-http-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
@Transactional
class FinanceStoreScopeHttpAcceptanceTest {

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
    @Autowired private ReceivableRepository receivableRepository;
    @Autowired private EntityManager entityManager;

    private Scope scope;

    @BeforeEach
    void setUp() {
        scope = createScope();
    }

    @Test
    void storeFinanceSeesOnlyOwnStoreOverHttp() throws Exception {
        String token = login(scope.storeFinancePhone());
        JsonNode response = api("finance/summary/report", "{}", token);
        assertThat(response.path("success").asBoolean()).as(response.toString()).isTrue();
        assertThat(response.path("data").path("receivableAmount").decimalValue())
                .isEqualByComparingTo("200.00");
        assertThat(response.path("data").path("projectCount").asInt()).isEqualTo(1);
    }

    @Test
    void headFinanceAndBossSeeAllStoresAndCanDrillDownOverHttp() throws Exception {
        String headToken = login(scope.headFinancePhone());
        String bossToken = login(scope.bossPhone());

        assertThat(api("finance/summary/report", "{}", headToken).path("data").path("receivableAmount").decimalValue())
                .isEqualByComparingTo("300.00");
        assertThat(api("finance/summary/report", "{}", bossToken).path("data").path("receivableAmount").decimalValue())
                .isEqualByComparingTo("300.00");

        String drill = "{\"storeId\":\"%s\"}".formatted(scope.storeBId());
        assertThat(api("finance/summary/report", drill, headToken).path("data").path("receivableAmount").decimalValue())
                .isEqualByComparingTo("200.00");
        assertThat(api("finance/summary/report", drill, bossToken).path("data").path("receivableAmount").decimalValue())
                .isEqualByComparingTo("200.00");
    }

    @Test
    void crossStoreAndCrossCompanyStoreAreRejectedOverHttp() throws Exception {
        String storeToken = login(scope.storeFinancePhone());
        String headToken = login(scope.headFinancePhone());

        // 门店范围下钻他店 → 拒绝
        JsonNode otherStore = api("finance/summary/report", "{\"storeId\":\"%s\"}".formatted(scope.storeAId()), storeToken);
        assertThat(otherStore.path("success").asBoolean()).isFalse();
        assertThat(otherStore.path("errorCode").path("code").asText()).isEqualTo("403");

        // 企业范围下钻他企业门店 → 拒绝
        JsonNode foreignStore = api("finance/summary/report",
                "{\"storeId\":\"%s\"}".formatted(scope.foreignStoreId()), headToken);
        assertThat(foreignStore.path("success").asBoolean()).isFalse();
        assertThat(foreignStore.path("errorCode").path("code").asText()).isEqualTo("403");
    }

    @Test
    void storeManagerCanBookkeepAndRecordLandsInOwnStoreOverHttp() throws Exception {
        String token = login(scope.storeManagerPhone());
        JsonNode created = api("finance/receivable/save", """
                {"projectId":%s,"name":"店长记账验收","stageName":"首期款","type":"First","amount":88.00,"dueDate":"2026-11-30"}
                """.formatted(scope.projectBId()), token);

        assertThat(created.path("success").asBoolean()).as(created.toString()).isTrue();
        assertThat(created.path("data").path("amount").decimalValue()).isEqualByComparingTo("88.00");

        // 归属核对：店长新记账单必须落在本店范围（换用门店财务账号按本店范围核对可见性）
        String financeToken = login(scope.storeFinancePhone());
        JsonNode page = api("finance/receivable/page", "{\"current\":1,\"size\":50}", financeToken);
        assertThat(page.path("success").asBoolean()).as(page.toString()).isTrue();
        JsonNode items = page.path("data").path("list");
        if (items.isMissingNode()) {
            items = page.path("data").path("content");
        }
        // 本店 2 笔（种子 200 + 店长新记 88），他店 100 不可见
        assertThat(items.size()).isEqualTo(2);
        assertThat(items).anySatisfy(item ->
                assertThat(item.path("amount").decimalValue()).isEqualByComparingTo("88.00"));
        assertThat(items).noneSatisfy(item ->
                assertThat(item.path("amount").decimalValue()).isEqualByComparingTo("100.00"));
    }

    /**
     * P1-a2 · 新补门店入参的 7 个页面（`DESIGN-BIZ-20261013-005` §1）：
     * 全部门店不传参 / 企业范围下钻本店 / 下钻他企业门店 403 / **门店范围角色（店长）传他店 403**。
     */
    @Test
    void newStoreScopePagesResolveStoreDimensionOverHttp() throws Exception {
        String bossToken = login(scope.bossPhone());
        String storeManagerToken = login(scope.storeManagerPhone());
        String storeFinanceToken = login(scope.storeFinancePhone());
        String[] endpoints = {
                "finance/refund/page", "payroll/sheet/page", "payroll/item/page", "payroll/commission/page",
                "payroll/store-payroll/page", "document/voucher/page", "finance/partner-ledger/report"
        };
        for (String endpoint : endpoints) {
            // 1) 全部门店：不传 storeId（绝不传状态值 '0'）
            JsonNode all = api(endpoint, "{\"current\":1,\"size\":20}", bossToken);
            assertThat(all.path("success").asBoolean()).as(endpoint + " 全部门店: " + all).isTrue();

            if ("payroll/store-payroll/page".equals(endpoint)) {
                JsonNode financeOwnStore = api(endpoint, "{\"current\":1,\"size\":20}", storeFinanceToken);
                assertThat(financeOwnStore.path("success").asBoolean())
                        .as(endpoint + " 门店财务本店: " + financeOwnStore).isTrue();
                JsonNode financeCrossStore = api(endpoint,
                        "{\"storeId\":\"%s\"}".formatted(scope.storeAId()), storeFinanceToken);
                assertThat(financeCrossStore.path("success").asBoolean())
                        .as(endpoint + " 门店财务跨店: " + financeCrossStore).isFalse();
                assertThat(financeCrossStore.path("errorCode").path("code").asText()).isEqualTo("403");
            }

            // 2) 下钻本店维度 → 通过（企业范围任意本店；门店范围角色为其本店）
            JsonNode drill = api(endpoint,
                    "{\"current\":1,\"size\":20,\"storeId\":\"%s\"}".formatted(scope.storeBId()), bossToken);
            assertThat(drill.path("success").asBoolean()).as(endpoint + " 下钻本店: " + drill).isTrue();

            // 3) 企业范围下钻他企业门店 → 403
            JsonNode foreign = api(endpoint, "{\"storeId\":\"%s\"}".formatted(scope.foreignStoreId()), bossToken);
            assertThat(foreign.path("success").asBoolean()).as(endpoint + " 他企业门店: " + foreign).isFalse();
            assertThat(foreign.path("errorCode").path("code").asText()).as(endpoint + " 他企业门店码").isEqualTo("403");

            // 4) 门店范围角色（店长）传他店 → 403（锁本店，不得静默忽略）
            JsonNode crossStore = api(endpoint, "{\"storeId\":\"%s\"}".formatted(scope.storeAId()), storeManagerToken);
            assertThat(crossStore.path("success").asBoolean()).as(endpoint + " 店长跨店: " + crossStore).isFalse();
            assertThat(crossStore.path("errorCode").path("code").asText()).as(endpoint + " 店长跨店码").isEqualTo("403");
        }
    }

    @Test
    void storeManagerCannotTouchAnotherStoreProjectOverHttp() throws Exception {
        String token = login(scope.storeManagerPhone());
        JsonNode rejected = api("finance/receivable/save", """
                {"projectId":%s,"name":"越权记账","stageName":"首期款","type":"First","amount":66.00,"dueDate":"2026-11-30"}
                """.formatted(scope.projectAId()), token);

        assertThat(rejected.path("success").asBoolean()).isFalse();
        assertThat(rejected.path("errorCode").path("code").asText()).isIn("403", "404");
    }

    @Test
    void designerIsRejectedFromFinanceWorkbenchOverHttp() throws Exception {
        String token = login(scope.designerPhone());
        JsonNode response = api("finance/summary/report", "{}", token);
        assertThat(response.path("success").asBoolean()).isFalse();
        assertThat(response.path("errorCode").path("code").asText()).isEqualTo("403");
    }

    @Test
    void anonymousIsRejectedOverHttp() throws Exception {
        JsonNode response = api("finance/summary/report", "{}", null);
        assertThat(response.path("success").asBoolean()).isFalse();
        assertThat(response.path("errorCode").path("code").asText())
                .isIn("401", "tokenIsUndefined", "tokenInvalid", "tokenExpired");
    }


    // ---------- helpers ----------

    private JsonNode api(String route, String body, String token) throws Exception {
        var request = post("/api/" + route)
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "finance-store-scope-acceptance")
                .content(body);
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
        assertThat(response.path("success").asBoolean()).as(response.toString()).isTrue();
        String token = response.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private Scope createScope() {
        long seq = System.nanoTime();
        Company company = company("财务门店范围HTTP验收", seq);
        Store storeA = store(company, "HTTP总店", true);
        Store storeB = store(company, "HTTP城北店", false);
        Company otherCompany = company("财务门店范围HTTP他企", seq + 7);
        Store foreignStore = store(otherCompany, "HTTP他企门店", false);

        Employee boss = employee(company, storeA, EmployeeRole.Boss, "HTTP负责人", seq, 1);
        Employee headFinance = employee(company, storeA, EmployeeRole.Finance, "HTTP总部财务", seq, 2);
        Employee storeFinance = employee(company, storeB, EmployeeRole.Finance, "HTTP门店财务", seq, 3);
        Employee storeManager = employee(company, storeB, EmployeeRole.StoreManager, "HTTP店长", seq, 4);
        Employee designer = employee(company, storeA, EmployeeRole.Designer, "HTTP设计师", seq, 5);

        Project projectA = project(company, storeA, boss, seq, 11, "100.00");
        Project projectB = project(company, storeB, boss, seq, 12, "200.00");
        entityManager.flush();

        return new Scope(storeA.getStoreId(), storeB.getStoreId(), foreignStore.getStoreId(),
                projectA.getProjectId(), projectB.getProjectId(),
                boss.getTelNo(), headFinance.getTelNo(), storeFinance.getTelNo(),
                storeManager.getTelNo(), designer.getTelNo());
    }

    private Company company(String title, long seq) {
        Company company = new Company();
        company.setTitle(title + Math.abs(seq % 1000));
        company.setName("财务门店范围HTTP企业");
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
        String phone = "3" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L));
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

    private Project project(Company company, Store store, Employee operator, long seq, int offset,
                            String receivableAmount) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(operator);
        customer.setName("HTTP客户" + offset);
        customer.setTelNo("4" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        customer.setArea(new BigDecimal("120"));
        customer.setAddr("财务门店范围HTTP地址");
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(operator);
        property.setCustomer(customer);
        property.setPropertyNo("PROP-HTTP-" + offset + "-" + Math.abs(seq % 100000));
        property.setAddress("财务门店范围HTTP小区 " + offset + " 栋 101");
        property.setArea(new BigDecimal("120"));
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("QUOTE-HTTP-" + offset + "-" + Math.abs(seq % 100000));
        quote.setTitle("财务门店范围HTTP报价");
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
        project.setNo("PROJECT-HTTP-" + offset + "-" + Math.abs(seq % 100000));
        project.setName("财务门店范围HTTP项目" + offset);
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
        project = projectRepository.save(project);

        Receivable receivable = new Receivable();
        receivable.setCompany(company);
        receivable.setStore(store);
        receivable.setOwner(operator);
        receivable.setProject(project);
        receivable.setCustomer(customer);
        receivable.setProperty(property);
        receivable.setName("HTTP首期款" + offset);
        receivable.setStageName("首期款");
        receivable.setType(ReceivableType.First);
        receivable.setAmount(new BigDecimal(receivableAmount));
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(LocalDate.now().plusDays(30));
        receivable.setStatus(ReceivableStatus.Pending);
        receivable.setCreatedBy(operator);
        receivableRepository.save(receivable);
        return project;
    }

    private record Scope(Long storeAId, Long storeBId, Long foreignStoreId,
                         Long projectAId, Long projectBId,
                         String bossPhone, String headFinancePhone, String storeFinancePhone,
                         String storeManagerPhone, String designerPhone) {
    }
}
