package com.th4.edge.decoboon.statistic.access;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.context.StatisticStoreAggregateContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统计域门店维度**聚合下推 SQL** 的口径验收（P0B-02 / `DEC-036` / `BR-031`）。
 *
 * <p>覆盖整改后不再由单元测试覆盖的 SQL 侧口径：门店条件、未归属计数、正常经营来源排除、
 * 已完工/已结算项目排除、客单分桶阈值（万元 HALF_UP 边界）、区域 Top-N、本月报价三分量。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statistic-store-aggregate;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/statistic-store-aggregate-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class StatisticStoreAggregateContextTest {

    private static final LocalDate MONTH = LocalDate.now().withDayOfMonth(1);

    @Autowired private StatisticStoreAggregateContext aggregate;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectContractRepository contractRepository;
    @Autowired private ReceivableRepository receivableRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private EntityManager entityManager;

    private Long comId;
    private Long storeAId;
    private Long storeBId;

    @BeforeEach
    void setUp() {
        long seq = System.nanoTime();
        Company company = new Company();
        company.setTitle("聚合口径" + Math.abs(seq % 1000));
        company.setName("聚合口径企业");
        company.setSize(Size.A);
        company = companyRepository.save(company);
        comId = company.getComId();

        Store storeA = store(company, "聚合总店", true);
        Store storeB = store(company, "聚合分店", false);
        storeAId = storeA.getStoreId();
        storeBId = storeB.getStoreId();
        Employee boss = employee(company, storeA, "聚合负责人", seq, 1);

        Customer cA = customer(company, storeA, boss, "聚合客户A", CustomerSource.Lead, "阳光小区", seq, 11);
        Customer cB = customer(company, storeB, boss, "聚合客户B", CustomerSource.Referral, "滨湖苑", seq, 12);
        customer(company, null, boss, "聚合未归属客户", CustomerSource.Other, null, seq, 13);

        // 合同与项目/房屋 1:1（库内 uk_project_contract_project、uk_project_company_property 均唯一）
        signedContract(company, storeA, cA, boss, new BigDecimal("99949.99"), MONTH, BusinessOrigin.New,
                ProjectStatus.Ongoing, seq, 21);
        signedContract(company, storeA, cA, boss, new BigDecimal("99950.00"), MONTH, BusinessOrigin.New,
                ProjectStatus.Ongoing, seq, 22);
        signedContract(company, storeA, cA, boss, new BigDecimal("260000.00"), MONTH, BusinessOrigin.New,
                ProjectStatus.Ongoing, seq, 23);
        // 非正常经营来源（接管）不计入驾驶舱签约额，但计入组织效能口径（与整改前一致）
        signedContract(company, storeA, cA, boss, new BigDecimal("700000.00"), MONTH, BusinessOrigin.Carryover,
                ProjectStatus.Ongoing, seq, 24);
        // 已完工项目不计入在建项目数
        signedContract(company, storeA, cA, boss, new BigDecimal("800000.00"), MONTH, BusinessOrigin.New,
                ProjectStatus.Finished, seq, 25);
        signedContract(company, storeB, cB, boss, new BigDecimal("300000.00"), MONTH, BusinessOrigin.New,
                ProjectStatus.Ongoing, seq, 26);

        receipt(company, storeA, cA, boss, new BigDecimal("40000.00"), LocalDate.now().minusDays(1), seq, 41);
        receipt(company, storeB, cB, boss, new BigDecimal("25000.00"), LocalDate.now().minusDays(2), seq, 42);

        quote(company, storeA, cA, boss, QuoteStatus.Submitted, seq, 51);
        quote(company, storeA, cA, boss, QuoteStatus.Accepted, seq, 52);
        quote(company, storeA, cA, boss, QuoteStatus.Draft, seq, 53);
        quote(company, storeB, cB, boss, QuoteStatus.Accepted, seq, 54);
        entityManager.flush();
    }

    @Test
    void signedContractAmountRespectsStoreScopeMonthAndNormalOrigin() {
        // A 店本月正常经营合同：99949.99 + 99950 + 260000 + 800000（接管 700000 被排除）
        assertThat(aggregate.signedContractAmount(comId, storeAId, MONTH)).isEqualByComparingTo("1259899.99");
        assertThat(aggregate.signedContractAmount(comId, storeBId, MONTH)).isEqualByComparingTo("300000.00");
        assertThat(aggregate.signedContractAmount(comId, 0L, MONTH)).isEqualByComparingTo("1559899.99");
        assertThat(aggregate.signedContractAmount(comId, storeAId, MONTH.minusMonths(1))).isEqualByComparingTo("0.00");
    }

    @Test
    void receiptAmountAndActiveProjectCountRespectStoreScope() {
        assertThat(aggregate.receiptAmount(comId, storeAId, MONTH)).isEqualByComparingTo("40000.00");
        assertThat(aggregate.receiptAmount(comId, 0L, MONTH)).isEqualByComparingTo("65000.00");

        // A 店 5 个项目中 4 个在建（1 个已完工不计入）；单店视图不含未归属项目
        assertThat(aggregate.activeProjectCount(comId, storeAId, MONTH)).isEqualTo(4);
        assertThat(aggregate.activeProjectCount(comId, storeBId, MONTH)).isEqualTo(1);
        assertThat(aggregate.activeProjectCount(comId, 0L, MONTH)).isEqualTo(5);
    }

    @Test
    void unassignedCountsAndMonthQuoteCountsAreSqlSide() {
        assertThat(aggregate.unassignedCustomers(comId)).isEqualTo(1);
        assertThat(aggregate.unassignedEmployees(comId)).isZero();

        // A 店本月报价：5 张随合同生成 + 3 张独立报价（已提交 / 已接受 / 草稿）
        StatisticStoreAggregateContext.QuoteCounts a = aggregate.monthQuoteCounts(comId, storeAId, MONTH);
        assertThat(a.total()).as("漏斗报价 = 本月报价单数（含草稿）").isEqualTo(8);
        assertThat(a.submitted()).as("已提交 = 排除草稿与替代").isEqualTo(7);
        assertThat(a.pendingDecision()).isEqualTo(1);

        StatisticStoreAggregateContext.QuoteCounts all = aggregate.monthQuoteCounts(comId, 0L, MONTH);
        assertThat(all.total()).as("全部门店含 B 店报价").isEqualTo(10);
        assertThat(all.submitted()).isEqualTo(9);
    }

    @Test
    void orderSizeBucketsUseWanHalfUpBoundariesAndRegionTopNLimits() {
        long[] buckets = aggregate.orderSizeBuckets(comId, storeAId, MONTH);
        assertThat(buckets[0]).as("99949.99 落在 10 万以下桶").isEqualTo(1);
        assertThat(buckets[1]).as("99950.00 因万元 HALF_UP 进位落在 10-20 万桶").isEqualTo(1);
        assertThat(buckets[2]).as("260000 落在 20-30 万桶").isEqualTo(1);
        assertThat(buckets[3]).isZero();
        assertThat(buckets[4]).as("800000 落在 50 万以上桶").isEqualTo(1);

        // 区域 Top-N：按签约额倒序 + LIMIT
        List<Object[]> regions = aggregate.regionMetrics(comId, storeAId, MONTH, 1);
        assertThat(regions).hasSize(1);
        assertThat(regions.get(0)[0]).isEqualTo("阳光小区");
    }

    @Test
    void organizationAggregatesOnlyCoverProjectStoreScope() {
        // 组织效能签约额按**项目门店**且不做来源过滤（含接管），与驾驶舱合同自身门店口径不同
        assertThat(aggregate.organizationSignedAmount(comId, storeAId, MONTH)).isEqualByComparingTo("1959899.99");
        assertThat(aggregate.organizationSignedAmount(comId, storeBId, MONTH)).isEqualByComparingTo("300000.00");
        assertThat(aggregate.organizationNewProjectCount(comId, storeAId, MONTH)).isEqualTo(5);

        StatisticStoreAggregateContext.OrganizationCounts counts = aggregate.organizationCounts(comId, storeAId, MONTH);
        assertThat(counts.headcount()).isEqualTo(1);
        assertThat(counts.newHires()).isEqualTo(1);
        assertThat(counts.roleCounts().get(EmployeeRole.Boss)).isEqualTo(1L);
    }

    // ---------- 夹具 ----------

    private Store store(Company company, String name, boolean defaultStore) {
        Store store = new Store();
        store.setCompany(company);
        store.setName(name);
        store.setDefaultStore(defaultStore);
        return storeRepository.save(store);
    }

    private Employee employee(Company company, Store store, String name, long seq, int offset) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(EmployeeRole.Boss);
        employee.setStatus(EmployeeStatus.Active);
        employee.setName(name);
        employee.setTelNo("2" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        employee.setJoinedAt(LocalDateTime.now().minusDays(1));
        return employeeRepository.save(employee);
    }

    private Customer customer(Company company, Store store, Employee owner, String name,
                             CustomerSource source, String community, long seq, int offset) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(owner);
        customer.setName(name);
        customer.setSource(source);
        customer.setCommunity(community);
        customer.setLastVisitAt(LocalDateTime.now().minusDays(1));
        customer.setTelNo("3" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        customer.setArea(new BigDecimal("120"));
        return customerRepository.save(customer);
    }

    /** 一个房屋 = 一个项目 = 一个合同（库内两份唯一约束）。 */
    private Project signedContract(Company company, Store store, Customer customer, Employee creator,
                                   BigDecimal amount, LocalDate signedAt, BusinessOrigin origin,
                                   ProjectStatus status, long seq, int offset) {
        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(creator);
        property.setCustomer(customer);
        property.setPropertyNo("PROP-AGG-" + offset + "-" + Math.abs(seq % 100000));
        property.setAddress("聚合口径小区 " + offset + " 栋 101");
        property.setCommunity("阳光小区");
        property.setArea(new BigDecimal("120"));
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("QUOTE-AGG-" + offset + "-" + Math.abs(seq % 100000));
        quote.setTitle("聚合口径报价");
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(creator);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(new BigDecimal("120"));
        quote.setSubtotal(amount);
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setAmount(amount);
        quote.setCost(amount);
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(QuoteStatus.Accepted);
        quote.setSubmittedAt(LocalDateTime.now().minusDays(2));
        quote = quoteRepository.save(quote);

        Project project = new Project();
        project.setNo("PROJECT-AGG-" + offset + "-" + Math.abs(seq % 100000));
        project.setName("聚合口径项目" + offset);
        project.setCompany(company);
        project.setStore(store);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setDesigner(creator);
        project.setManager(creator);
        project.setSupport(creator);
        project.setOwner(creator);
        project.setSiteAddr(property.getAddress());
        project.setArea(new BigDecimal("120"));
        project.setContractAmount(amount);
        project.setCost(amount);
        project.setStatus(status);
        project.setProgress(10);
        project.setStartDate(LocalDate.now().minusDays(2));
        project.setPlanEndDate(LocalDate.now().plusDays(60));
        project = projectRepository.save(project);

        ProjectContract contract = new ProjectContract();
        contract.setNo("CONTRACT-AGG-" + offset + "-" + Math.abs(seq % 100000));
        contract.setCompany(company);
        contract.setStore(store);
        contract.setProject(project);
        contract.setCustomer(customer);
        contract.setProperty(property);
        contract.setQuote(quote);
        contract.setCreatedBy(creator);
        contract.setBaselineAmount(amount);
        contract.setCurrentAmount(amount);
        contract.setStatus(ContractStatus.Signed);
        contract.setSignedAt(signedAt);
        contract.setBusinessOrigin(origin);
        contractRepository.save(contract);
        return project;
    }

    private void receipt(Company company, Store store, Customer customer, Employee owner,
                         BigDecimal amount, LocalDate receivedAt, long seq, int offset) {
        Project project = projectRepository.findByCompany_ComIdOrderByProjectIdDesc(company.getComId()).stream()
                .filter(item -> item.getStore() != null && item.getStore().getStoreId().equals(store.getStoreId()))
                .findFirst().orElseThrow();

        Receivable receivable = new Receivable();
        receivable.setCompany(company);
        receivable.setStore(store);
        receivable.setProject(project);
        receivable.setProperty(project.getProperty());
        receivable.setCustomer(customer);
        receivable.setOwner(owner);
        receivable.setCreatedBy(owner);
        receivable.setName("聚合口径应收 " + offset);
        receivable.setType(ReceivableType.First);
        receivable.setAmount(amount);
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(LocalDate.now().plusDays(10));
        receivable.setStatus(ReceivableStatus.Pending);
        receivable.setInvoiceRequired(false);
        receivable = receivableRepository.save(receivable);

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
        receipt.setIdempotencyKey("AGG-" + offset + "-" + Math.abs(seq % 100000));
        receiptRepository.save(receipt);
    }

    private void quote(Company company, Store store, Customer customer, Employee creator,
                       QuoteStatus status, long seq, int offset) {
        Property property = propertyRepository
                .findByCompany_ComIdAndCustomer_CustIdIn(company.getComId(), List.of(customer.getCustId()))
                .stream().findFirst().orElseGet(() -> {
                    Property created = new Property();
                    created.setCompany(company);
                    created.setStore(store);
                    created.setOwner(creator);
                    created.setCustomer(customer);
                    created.setPropertyNo("PROP-Q-" + offset + "-" + Math.abs(seq % 100000));
                    created.setAddress("聚合口径报价房屋 " + offset);
                    created.setArea(new BigDecimal("90"));
                    created.setStatus(PropertyStatus.Serving);
                    return propertyRepository.save(created);
                });
        Quote quote = new Quote();
        quote.setNo("QUOTE-ONLY-" + offset + "-" + Math.abs(seq % 100000));
        quote.setTitle("聚合口径报价");
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(creator);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(new BigDecimal("120"));
        quote.setSubtotal(BigDecimal.ZERO);
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setAmount(BigDecimal.ZERO);
        quote.setCost(BigDecimal.ZERO);
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(status);
        quote.setSubmittedAt(LocalDateTime.now().minusDays(1));
        quoteRepository.save(quote);
    }
}
