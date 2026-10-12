package com.th4.edge.decoboon.common.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * DEC-067 门店维度「绑定改造 + 哨兵兼容」HTTP 级验收（P0B-01，`GAP-048`）。
 *
 * <p>覆盖 7 个 Provider：{@code statistic/boss/dashboard}、{@code statistic/boss/business/report}、
 * {@code statistic/organization/report}、{@code statistic/performance/report}、
 * {@code statistic/boss/measure/report}、{@code customer/customer/page}、{@code project/project/page}。</p>
 *
 * <ul>
 *   <li>门店维度经 provider 的 {@code @AutoInject} 字段绑定 → 真实 HTTP 传参必须生效（非法门店 403）；</li>
 *   <li>{@code storeId = 0} 与不传等价（防「传 0 静默返回空」回归）；</li>
 *   <li>单店视图确实收窄（客户/项目分页总数）。</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:store-dimension-binding;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/store-dimension-binding-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
@Transactional
class StoreDimensionBindingHttpAcceptanceTest {

    private static final String PASSWORD = "Accept1234";

    private static final String[] STORE_AWARE_ROUTES = {
            "statistic/boss/dashboard",
            "statistic/boss/business/report",
            "statistic/organization/report",
            "statistic/performance/report",
            "statistic/boss/measure/report",
            "customer/customer/page",
            "project/project/page"
    };

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

    private com.th4.edge.tenant.user.model.Company company;
    private Long storeAId;
    private Long storeBId;
    private Long foreignStoreId;
    private String bossPhone;

    @BeforeEach
    void setUp() {
        long seq = System.nanoTime();
        company = company("门店维度绑定验收", seq);
        Store storeA = store(company, "绑定总店", true);
        Store storeB = store(company, "绑定城北店", false);
        Company other = company("门店维度绑定他企", seq + 7);
        Store foreign = store(other, "绑定他企门店", false);
        storeAId = storeA.getStoreId();
        storeBId = storeB.getStoreId();
        foreignStoreId = foreign.getStoreId();

        Employee boss = employee(company, storeA, EmployeeRole.Boss, "绑定负责人", seq, 1);
        employee(company, storeA, EmployeeRole.Designer, "绑定设计师", seq, 2);
        employee(company, storeB, EmployeeRole.StoreManager, "绑定店长", seq, 3);
        bossPhone = boss.getTelNo();

        project(company, storeA, boss, seq, 11);
        project(company, storeB, boss, seq, 12);
        entityManager.flush();
    }

    @Test
    void invalidOrForeignStoreIsRejectedOnEveryRoute() throws Exception {
        String token = login(bossPhone);
        for (String route : STORE_AWARE_ROUTES) {
            JsonNode missing = api(route, encode(0, 999999999999L), token);
            assertThat(missing.path("success").asBoolean())
                    .as("%s 传不存在的门店必须被拒绝（绑定生效），实际：%s", route, missing)
                    .isFalse();
            assertThat(missing.path("errorCode").path("code").asText()).as(route).isEqualTo("403");

            JsonNode foreign = api(route, encode(0, foreignStoreId), token);
            assertThat(foreign.path("success").asBoolean())
                    .as("%s 传他企业门店必须被拒绝，实际：%s", route, foreign)
                    .isFalse();
            assertThat(foreign.path("errorCode").path("code").asText()).as(route).isEqualTo("403");
        }
    }

    @Test
    void zeroStoreIdIsEquivalentToOmittedOnEveryRoute() throws Exception {
        String token = login(bossPhone);
        for (String route : STORE_AWARE_ROUTES) {
            JsonNode omitted = api(route, "{}", token);
            JsonNode zero = api(route, "{\"storeId\":0}", token);
            assertThat(omitted.path("success").asBoolean()).as("%s 不传门店应成功：%s", route, omitted).isTrue();
            assertThat(zero.path("success").asBoolean()).as("%s 传 0 应成功（不静默返回空）：%s", route, zero).isTrue();
            assertThat(stable(zero.path("data")).toString())
                    .as("%s「传 0」必须与「不传」结果一致", route)
                    .isEqualTo(stable(omitted.path("data")).toString());
        }
    }

    @Test
    void singleStoreViewNarrowsCustomerAndProjectPages() throws Exception {
        String token = login(bossPhone);

        assertThat(countOf(api("customer/customer/page", "{}", token))).isEqualTo(2);
        assertThat(countOf(api("customer/customer/page", encode(0, storeBId), token))).isEqualTo(1);
        assertThat(countOf(api("customer/customer/page", encode(0, storeAId), token))).isEqualTo(1);

        assertThat(countOf(api("project/project/page", "{}", token))).isEqualTo(2);
        assertThat(countOf(api("project/project/page", encode(0, storeBId), token))).isEqualTo(1);
        assertThat(countOf(api("project/project/page", "{\"storeId\":0}", token))).isEqualTo(2);
    }

    @Test
    void singleStoreViewChangesStatisticPayloads() throws Exception {
        String token = login(bossPhone);
        for (String route : new String[]{
                "statistic/boss/dashboard", "statistic/boss/business/report",
                "statistic/organization/report", "statistic/performance/report",
                "statistic/boss/measure/report"}) {
            JsonNode all = api(route, "{}", token);
            JsonNode single = api(route, encode(0, storeBId), token);
            assertThat(single.path("success").asBoolean()).as("%s 单店视图应成功：%s", route, single).isTrue();
            assertThat(single.path("data").toString())
                    .as("%s 单店视图必须与全部门店不同（证明门店维度生效）", route)
                    .isNotEqualTo(all.path("data").toString());
        }
    }

    @Test
    void measureFiltersBindThroughAutoInject() throws Exception {
        // GAP-048 收尾：measure/report 的 status/month/marketerId/source/keyword/designerId 也必须经 HTTP 生效
        String token = login(bossPhone);
        JsonNode all = api("statistic/boss/measure/report", "{}", token);
        int total = all.path("data").path("items").size();
        assertThat(total).as("未加筛选时应返回全部量房事项").isEqualTo(2);

        JsonNode notScheduled = api("statistic/boss/measure/report", "{\"status\":\"notScheduled\"}", token);
        assertThat(notScheduled.path("data").path("items").size()).as("未预约量房应仍返回全部").isEqualTo(2);

        JsonNode scheduled = api("statistic/boss/measure/report", "{\"status\":\"scheduled\"}", token);
        assertThat(scheduled.path("data").path("items").size())
                .as("status 筛选必须生效（修复前未绑定 → 仍返回全部）").isZero();

        JsonNode storeA = api("statistic/boss/measure/report", encode(0, storeAId), token);
        assertThat(storeA.path("data").path("items").size()).as("门店维度仍生效").isEqualTo(1);
    }

    @Test
    void storeScopeAnnotationAndUnassignedIdentity() throws Exception {
        // 造一条 store 为空的客户：全部门店必须计入并显式标注，单店视图不含（DEC-067 BOSS-STORE-03）
        Customer orphan = new Customer();
        orphan.setCompany(company);
        orphan.setName("未归属门店客户");
        orphan.setTelNo("13900009999");
        orphan.setArea(new BigDecimal("88"));
        orphan.setAddr("门店维度绑定未归属地址");
        customerRepository.save(orphan);
        entityManager.flush();

        String token = login(bossPhone);
        JsonNode all = api("statistic/boss/business/report", "{}", token);
        JsonNode allScope = all.path("data").path("storeScope");
        assertThat(allScope.path("scopeType").asText()).isEqualTo("AllStores");
        assertThat(allScope.path("unassignedCount").asInt()).as("全部门店应标注未归属条数").isEqualTo(1);
        int allLead = all.path("data").path("funnel").get(0).path("count").asInt();

        JsonNode storeA = api("statistic/boss/business/report", encode(0, storeAId), token);
        JsonNode storeAScope = storeA.path("data").path("storeScope");
        assertThat(storeAScope.path("scopeType").asText()).isEqualTo("Store");
        assertThat(storeAScope.path("storeId").asLong()).isEqualTo(storeAId);
        assertThat(storeAScope.path("storeName").asText()).isNotBlank();
        assertThat(storeAScope.path("unassignedCount").asInt()).as("单店视图不含未归属").isZero();
        int storeALead = storeA.path("data").path("funnel").get(0).path("count").asInt();

        int storeBLead = api("statistic/boss/business/report", encode(0, storeBId), token)
                .path("data").path("funnel").get(0).path("count").asInt();

        // 恒等式：合计 = Σ各门店 + 未归属门店
        assertThat(allLead).as("合计 = Σ门店 + 未归属（%s = %s + %s + 1）", allLead, storeALead, storeBLead)
                .isEqualTo(storeALead + storeBLead + 1);
    }

    // ---------- helpers ----------

    private String encode(long ignoredMonth, Long storeId) {
        return "{\"storeId\":" + storeId + "}";
    }

    /**
     * 去掉与门店维度无关的易变字段（如量房报告的第二级时间戳 {@code asOf}），
     * 只比较门店维度相关的数据，避免跨秒造成的假失败。
     */
    private JsonNode stable(JsonNode data) {
        JsonNode copy = data.deepCopy();
        if (copy.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) copy).remove(java.util.List.of("asOf", "generatedAt", "now"));
        }
        return copy;
    }

    /** 分页 Provider 的条数：兼容 {@code list}/{@code content} 与 {@code total}/{@code totalElements}。 */
    private int countOf(JsonNode response) {
        JsonNode data = response.path("data");
        JsonNode items = data.path("list");
        if (items.isMissingNode()) items = data.path("content");
        if (!items.isMissingNode()) return items.size();
        JsonNode total = data.path("total");
        if (total.isMissingNode()) total = data.path("totalElements");
        return total.asInt();
    }

    private JsonNode api(String route, String body, String token) throws Exception {
        var request = post("/api/" + route)
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "store-dimension-binding-acceptance")
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

    private Company company(String title, long seq) {
        Company company = new Company();
        company.setTitle(title + Math.abs(seq % 1000));
        company.setName("门店维度绑定企业");
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
        String phone = "5" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L));
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

    private void project(Company company, Store store, Employee operator, long seq, int offset) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(operator);
        customer.setName("绑定客户" + offset);
        customer.setTelNo("6" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        customer.setArea(new BigDecimal("120"));
        customer.setAddr("门店维度绑定地址");
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(operator);
        property.setCustomer(customer);
        property.setPropertyNo("PROP-BIND-" + offset + "-" + Math.abs(seq % 100000));
        property.setAddress("门店维度绑定小区 " + offset + " 栋 101");
        property.setArea(new BigDecimal("120"));
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("QUOTE-BIND-" + offset + "-" + Math.abs(seq % 100000));
        quote.setTitle("门店维度绑定报价");
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
        project.setNo("PROJECT-BIND-" + offset + "-" + Math.abs(seq % 100000));
        project.setName("门店维度绑定项目" + offset);
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
