package com.th4.edge.decoboon.statistic.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 统计域门店维度整改的**口径金标采集**（P0B-02 / `BR-031` 判定要点 6「口径不变」）。
 *
 * <p>本测试不写断言，只把 4 个 Provider 在「全部门店 / A 店 / B 店」三种范围下的响应
 * 规范化后写入 {@code target/scope-golden/<dir>/<provider>-<scope>.json}；
 * 整改前用同一夹具跑一次（{@code -Dscope.golden.dir=before}），整改后跑一次（{@code after}），
 * 用 {@code diff -r} 证明「金额、计数、比率、排名」等对外数字完全一致。</p>
 *
 * <p>夹具刻意覆盖：两门店 + 未归属门店记录、多状态报价（草稿/已提交/已接受/已拒绝）、
 * 本月与上月合同与收款、应付、已完工项目、验收单 —— 保证三种范围的数字互不相同。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statistic-scope-golden;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/statistic-scope-golden-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
@Transactional
class StatisticScopeGoldenCaptureTest {

    private static final String PASSWORD = "Accept1234";
    private static final List<String> PROVIDERS = List.of(
            "statistic/boss/dashboard",
            "statistic/boss/business/report",
            "statistic/organization/report",
            "statistic/performance/report");

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
    @Autowired private ProjectContractRepository contractRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private PayableRepository payableRepository;
    @Autowired private ReceivableRepository receivableRepository;
    @Autowired private AcceptanceRepository acceptanceRepository;
    @Autowired private ProjectStageRepository stageRepository;
    @Autowired private EntityManager entityManager;

    private Long storeAId;
    private Long storeBId;
    private String bossPhone;

    @BeforeEach
    void setUp() {
        long seq = System.nanoTime();
        LocalDate month = LocalDate.now().withDayOfMonth(1);
        Company company = company("口径金标", seq);
        Store storeA = store(company, "金标总店", true);
        Store storeB = store(company, "金标城北店", false);
        storeAId = storeA.getStoreId();
        storeBId = storeB.getStoreId();

        Employee boss = employee(company, storeA, EmployeeRole.Boss, "金标负责人", seq, 1);
        bossPhone = boss.getTelNo();
        Employee designer = employee(company, storeA, EmployeeRole.Designer, "金标设计师", seq, 2);
        Employee marketer = employee(company, storeA, EmployeeRole.Marketer, "金标业务员", seq, 3);
        Employee pm = employee(company, storeA, EmployeeRole.Pm, "金标项目经理", seq, 4);
        employee(company, storeB, EmployeeRole.StoreManager, "金标店长", seq, 5);

        // —— 客户（A 店 2、B 店 1、未归属 1）——
        Customer cA1 = customer(company, storeA, boss, marketer, "金标客户A1", CustStatus.Following, seq, 11);
        cA1.setSource(CustomerSource.Lead);
        cA1.setCommunity("阳光小区");
        cA1.setLastVisitAt(LocalDateTime.now().minusDays(3));
        cA1.setMeasureAt(LocalDateTime.now().minusDays(2));
        customerRepository.save(cA1);
        Customer cA2 = customer(company, storeA, boss, marketer, "金标客户A2", CustStatus.Signed, seq, 12);
        cA2.setSource(CustomerSource.Referral);
        cA2.setCommunity("阳光小区");
        customerRepository.save(cA2);
        Customer cB1 = customer(company, storeB, boss, marketer, "金标客户B1", CustStatus.Serving, seq, 13);
        cB1.setSource(CustomerSource.WalkIn);
        cB1.setCommunity("滨湖苑");
        customerRepository.save(cB1);
        Customer cNull = customer(company, null, boss, marketer, "金标未归属客户", CustStatus.New, seq, 14);
        customerRepository.save(cNull);

        Property pA1 = property(company, storeA, cA1, boss, seq, 21);
        Property pA2 = property(company, storeA, cA2, boss, seq, 22);
        Property pB1 = property(company, storeB, cB1, boss, seq, 23);
        Property pNull = property(company, null, cNull, boss, seq, 24);

        // —— 报价（机会口径：客户 + 房屋）——
        quote(company, storeA, cA1, pA1, designer, QuoteStatus.Submitted, 1, seq, 31);
        quote(company, storeA, cA1, pA1, designer, QuoteStatus.Accepted, 2, seq, 32);
        quote(company, storeA, cA2, pA2, designer, QuoteStatus.Rejected, 1, seq, 33);
        quote(company, storeA, cA1, pA1, designer, QuoteStatus.Draft, 1, seq, 34);
        quote(company, storeB, cB1, pB1, designer, QuoteStatus.Accepted, 1, seq, 35);
        quote(company, null, cNull, pNull, designer, QuoteStatus.Accepted, 1, seq, 36);

        // —— 项目 + 合同（A 店本月/上月、B 店本月、未归属本月）——
        Project prA1 = project(company, storeA, cA1, pA1, designer, pm, "金标项目A1", ProjectStatus.Ongoing,
                LocalDate.now().minusDays(10), null, seq, 41);
        Project prA2 = project(company, storeA, cA2, pA2, designer, pm, "金标项目A2", ProjectStatus.Finished,
                month.minusMonths(6), month.minusMonths(1).withDayOfMonth(20), seq, 42);
        Project prB1 = project(company, storeB, cB1, pB1, designer, pm, "金标项目B1", ProjectStatus.Ongoing,
                LocalDate.now().minusDays(5), null, seq, 43);
        Project prNull = project(company, null, cNull, pNull, designer, pm, "金标项目未归属", ProjectStatus.Preparing,
                LocalDate.now().minusDays(2), null, seq, 44);

        contract(company, storeA, prA1, cA1, pA1, boss, new BigDecimal("100000"), BigDecimal.ZERO, month, seq, 51);
        contract(company, storeA, prA2, cA2, pA2, boss, BigDecimal.ZERO, new BigDecimal("200000"),
                month.minusMonths(1), seq, 52);
        contract(company, storeB, prB1, cB1, pB1, boss, new BigDecimal("300000"), BigDecimal.ZERO, month, seq, 53);
        contract(company, null, prNull, cNull, pNull, boss, new BigDecimal("50000"), BigDecimal.ZERO, month, seq, 54);

        // —— 收款（A 店本月/上月、B 店本月）——
        receivable(company, storeA, prA1, cA1, pA1, boss, new BigDecimal("40000"), seq, 61);
        receivable(company, storeB, prB1, cB1, pB1, boss, new BigDecimal("25000"), seq, 62);
        Receivable reA = receivable(company, storeA, prA1, cA1, pA1, boss, new BigDecimal("10000"), seq, 63);
        receipt(company, storeA, prA1, cA1, reA, boss, new BigDecimal("40000"), LocalDate.now().minusDays(1), seq, 71);
        Receivable reB = receivable(company, storeB, prB1, cB1, pB1, boss, new BigDecimal("25000"), seq, 64);
        receipt(company, storeB, prB1, cB1, reB, boss, new BigDecimal("25000"), LocalDate.now().minusDays(2), seq, 72);
        Receivable rePrev = receivable(company, storeA, prA1, cA1, pA1, boss, new BigDecimal("10000"), seq, 65);
        receipt(company, storeA, prA1, cA1, rePrev, boss, new BigDecimal("10000"),
                month.minusMonths(1).withDayOfMonth(15), seq, 73);

        // —— 应付（成本完整性用）——
        payable(company, storeA, prA1, pA1, boss, new BigDecimal("60000"), seq, 81);
        payable(company, storeB, prB1, pB1, boss, new BigDecimal("15000"), seq, 82);

        // —— 验收单（项目经理准时交付/通过率）——
        acceptance(company, prA1, pA1, boss, AcceptStatus.Confirmed, seq, 91);
        acceptance(company, prB1, pB1, boss, AcceptStatus.Rejected, seq, 92);

        entityManager.flush();
    }

    @Test
    void captureScopeGolden() throws Exception {
        String token = login(bossPhone);
        LocalDate month = LocalDate.now().withDayOfMonth(1);
        String body = "{\"month\":\"%s\",\"topN\":5}".formatted(month);
        Path dir = Path.of("target", "scope-golden", System.getProperty("scope.golden.dir", "run"));
        Files.createDirectories(dir);

        List<String> scopes = new ArrayList<>();
        scopes.add("all");
        for (String provider : PROVIDERS) {
            for (String scope : scopes) {
                writeJson(dir.resolve(fileName(provider, "all")), api(provider, body, token).path("data"));
            }
            writeJson(dir.resolve(fileName(provider, "A")), api(provider, withStore(body, storeAId), token).path("data"));
            writeJson(dir.resolve(fileName(provider, "B")), api(provider, withStore(body, storeBId), token).path("data"));
        }
        System.out.println("[scope-golden] 已写入 " + dir.toAbsolutePath());
    }

    private String withStore(String body, Long storeId) {
        return body.substring(0, body.length() - 1) + ",\"storeId\":" + storeId + "}";
    }

    private String fileName(String provider, String scope) {
        return provider.replace('/', '-') + "-" + scope + ".json";
    }

    /** 规范化：键排序 + 去掉易变时间字段，保证可机械 diff。 */
    private void writeJson(Path file, JsonNode data) throws Exception {
        JsonNode copy = data.deepCopy();
        if (copy.isObject()) {
            ((ObjectNode) copy).remove(List.of("asOf", "generatedAt", "now", "updatedAt"));
        }
        ObjectMapper sorted = objectMapper.copy();
        String json = sorted.writerWithDefaultPrettyPrinter().writeValueAsString(copy);
        Files.writeString(file, json + "\n");
    }

    private JsonNode api(String route, String body, String token) throws Exception {
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

    private String login(String phone) throws Exception {
        JsonNode response = api0("auth/session/password-login",
                "{\"phone\":\"%s\",\"password\":\"%s\"}".formatted(phone, PASSWORD), null);
        return response.path("data").path("token").asText();
    }

    private JsonNode api0(String route, String body, String token) throws Exception {
        var request = post("/api/" + route).contentType(MediaType.APPLICATION_JSON).content(body);
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
        company.setName("口径金标企业");
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
        employee.setJoinedAt(LocalDateTime.now().minusDays(2));
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

    private Customer customer(Company company, Store store, Employee owner, Employee designer,
                             String name, CustStatus status, long seq, int offset) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(owner);
        customer.setDesigner(designer);
        customer.setName(name);
        customer.setStatus(status);
        customer.setTelNo("4" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        customer.setArea(new BigDecimal("120"));
        customer.setAddr("口径金标地址 " + offset);
        return customerRepository.save(customer);
    }

    private Property property(Company company, Store store, Customer customer, Employee owner, long seq, int offset) {
        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(owner);
        property.setCustomer(customer);
        property.setPropertyNo("PROP-GOLD-" + offset + "-" + Math.abs(seq % 100000));
        property.setAddress("口径金标小区 " + offset + " 栋 101");
        property.setCommunity("阳光小区");
        property.setArea(new BigDecimal("120"));
        property.setStatus(PropertyStatus.Serving);
        return propertyRepository.save(property);
    }

    private Quote quote(Company company, Store store, Customer customer, Property property, Employee designer,
                       QuoteStatus status, int version, long seq, int offset) {
        Quote quote = new Quote();
        quote.setNo("QUOTE-GOLD-" + offset + "-" + Math.abs(seq % 100000));
        quote.setTitle("口径金标报价");
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(designer);
        quote.setVersion(version);
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
        quote.setBusinessOrigin(BusinessOrigin.New);
        quote.setStatus(status);
        quote.setSubmittedAt(LocalDateTime.now().minusDays(1));
        if (status == QuoteStatus.Accepted || status == QuoteStatus.Converted) {
            quote.setAcceptedAt(LocalDateTime.now().minusDays(1));
        } else if (status == QuoteStatus.Rejected) {
            quote.setRejectedAt(LocalDateTime.now().minusDays(1));
        }
        return quoteRepository.save(quote);
    }

    private Project project(Company company, Store store, Customer customer, Property property,
                           Employee designer, Employee pm, String name, ProjectStatus status,
                           LocalDate startDate, LocalDate actualEndDate, long seq, int offset) {
        Project project = new Project();
        project.setNo("PROJECT-GOLD-" + offset + "-" + Math.abs(seq % 100000));
        project.setName(name);
        project.setCompany(company);
        project.setStore(store);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(company.getComId()).stream()
                .filter(item -> item.getCustomer() != null
                        && item.getCustomer().getCustId().equals(customer.getCustId()))
                .findFirst().orElseThrow());
        project.setDesigner(designer);
        project.setManager(pm);
        project.setSupport(pm);
        project.setOwner(companyOwner(company));
        project.setSiteAddr(property.getAddress());
        project.setArea(new BigDecimal("120"));
        project.setContractAmount(new BigDecimal("100000"));
        project.setCost(new BigDecimal("80000"));
        project.setStatus(status);
        project.setProgress(20);
        project.setStartDate(startDate);
        project.setPlanEndDate(actualEndDate == null ? LocalDate.now().plusDays(60) : actualEndDate.minusDays(3));
        project.setActualEndDate(actualEndDate);
        return projectRepository.save(project);
    }

    private Employee companyOwner(Company company) {
        return employeeRepository.findByCompany_ComId(company.getComId()).stream()
                .filter(item -> item.getRole() == EmployeeRole.Boss).findFirst().orElseThrow();
    }

    private ProjectContract contract(Company company, Store store, Project project, Customer customer, Property property,
                                    Employee creator, BigDecimal currentAmount, BigDecimal baselineAmount,
                                    LocalDate signedAt, long seq, int offset) {
        ProjectContract contract = new ProjectContract();
        contract.setNo("CONTRACT-GOLD-" + offset + "-" + Math.abs(seq % 100000));
        contract.setCompany(company);
        contract.setStore(store);
        contract.setProject(project);
        contract.setCustomer(customer);
        contract.setProperty(property);
        contract.setQuote(project.getQuote());
        contract.setCreatedBy(creator);
        contract.setBaselineAmount(baselineAmount);
        contract.setCurrentAmount(currentAmount);
        contract.setStatus(ContractStatus.Signed);
        contract.setSignedAt(signedAt);
        contract.setBusinessOrigin(BusinessOrigin.New);
        return contractRepository.save(contract);
    }

    private Receivable receivable(Company company, Store store, Project project, Customer customer, Property property,
                                 Employee owner, BigDecimal amount, long seq, int offset) {
        Receivable receivable = new Receivable();
        receivable.setCompany(company);
        receivable.setStore(store);
        receivable.setProject(project);
        receivable.setCustomer(customer);
        receivable.setProperty(property);
        receivable.setOwner(owner);
        receivable.setCreatedBy(owner);
        receivable.setName("口径金标应收 " + offset);
        receivable.setType(ReceivableType.First);
        receivable.setAmount(amount);
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(LocalDate.now().plusDays(10));
        receivable.setStatus(ReceivableStatus.Pending);
        receivable.setInvoiceRequired(false);
        return receivableRepository.save(receivable);
    }

    private Receipt receipt(Company company, Store store, Project project, Customer customer, Receivable receivable,
                           Employee owner, BigDecimal amount, LocalDate receivedAt, long seq, int offset) {
        Receipt receipt = new Receipt();
        receipt.setCompany(company);
        receipt.setStore(store);
        receipt.setProject(project);
        receipt.setCustomer(customer);
        receipt.setReceivable(receivable);
        receipt.setOwner(owner);
        receipt.setCreatedBy(owner);
        receipt.setAmount(amount);
        receipt.setAccount(FundAccount.CompanyBank);
        receipt.setReceivedAt(receivedAt);
        receipt.setIdempotencyKey("GOLD-" + offset + "-" + Math.abs(seq % 100000));
        return receiptRepository.save(receipt);
    }

    private Payable payable(Company company, Store store, Project project, Property property,
                           Employee owner, BigDecimal amount, long seq, int offset) {
        Payable payable = new Payable();
        payable.setCompany(company);
        payable.setStore(store);
        payable.setProject(project);
        payable.setProperty(property);
        payable.setOwner(owner);
        payable.setCreatedBy(owner);
        payable.setName("口径金标应付 " + offset);
        payable.setType(PayableType.Material);
        payable.setAmount(amount);
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setStatus(PayableStatus.Pending);
        payable.setUrgent(false);
        payable.setBusinessOrigin(BusinessOrigin.New);
        payable.setDueDate(LocalDate.now().plusDays(20));
        return payableRepository.save(payable);
    }

    private ProjectStage stage(Project project, Employee owner) {
        ProjectStage stage = new ProjectStage();
        stage.setCompany(project.getCompany());
        stage.setStore(project.getStore());
        stage.setProject(project);
        stage.setProperty(project.getProperty());
        stage.setOwner(owner);
        stage.setName("开工");
        stage.setStageCode("start");
        stage.setSort(1);
        stage.setWeight(BigDecimal.valueOf(100));
        stage.setStatus(StageStatus.Done);
        stage.setProgress(100);
        return stageRepository.save(stage);
    }

    private Acceptance acceptance(Company company, Project project, Property property, Employee owner,
                                  AcceptStatus status, long seq, int offset) {
        Acceptance acceptance = new Acceptance();
        acceptance.setCompany(company);
        acceptance.setStore(project.getStore());
        acceptance.setProject(project);
        acceptance.setProperty(property);
        acceptance.setOwner(owner);
        acceptance.setStage(stage(project, owner));
        acceptance.setStatus(status);
        if (status == AcceptStatus.Confirmed) {
            acceptance.setConfirmedAt(LocalDateTime.now().minusDays(1));
        }
        return acceptanceRepository.save(acceptance);
    }
}
