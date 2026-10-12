package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.model.FinanceSummary;
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
import com.th4.edge.framework.exception.DefinedException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DEC-065 财务门店数据范围：企业范围（BOSS / 总部财务）、门店范围（门店财务 / 店长）、
 * 门店维度下钻与跨店/跨企业越权负向，以及店长记账写权限边界。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-store-scope;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "spring.main.banner-mode=off",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/finance-store-scope-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class FinanceStoreScopeTest {

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private QuoteRepository quoteRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ReceivableRepository receivableRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private FinanceAccessContext accessContext;
    @Autowired
    private FinanceReportContext reportContext;

    private Scope scope;

    @BeforeEach
    void setUp() {
        scope = createScope();
    }

    @Test
    void storeFinanceSeesOnlyOwnStore() {
        FinanceSummary summary = reportContext.summary(scope.storeFinance(), null);
        assertThat(summary.getReceivableAmount()).isEqualByComparingTo("200.00");
        assertThat(summary.getProjectCount()).isEqualTo(1);
    }

    @Test
    void storeManagerSeesOnlyOwnStore() {
        FinanceSummary summary = reportContext.summary(scope.storeManagerB(), null);
        assertThat(summary.getReceivableAmount()).isEqualByComparingTo("200.00");
        assertThat(summary.getProjectCount()).isEqualTo(1);
    }

    @Test
    void headFinanceAndBossSeeAllStoresAndCanDrillDown() {
        assertThat(reportContext.summary(scope.headFinance(), null).getReceivableAmount())
                .isEqualByComparingTo("300.00");
        assertThat(reportContext.summary(scope.boss(), null).getReceivableAmount())
                .isEqualByComparingTo("300.00");

        assertThat(reportContext.summary(scope.headFinance(), scope.storeB().getStoreId()).getReceivableAmount())
                .isEqualByComparingTo("200.00");
        assertThat(reportContext.summary(scope.boss(), scope.storeB().getStoreId()).getReceivableAmount())
                .isEqualByComparingTo("200.00");
    }

    @Test
    void storeScopeCannotRequestAnotherStore() {
        // 缺省为本店
        assertThat(accessContext.resolveStoreFilter(scope.storeFinance(), null))
                .isEqualTo(scope.storeB().getStoreId());
        // 本店可直接传入
        assertThat(accessContext.resolveStoreFilter(scope.storeFinance(), scope.storeB().getStoreId()))
                .isEqualTo(scope.storeB().getStoreId());
        // 传入他店一律拒绝，不得静默忽略
        assertThatThrownBy(() -> accessContext.resolveStoreFilter(scope.storeFinance(), scope.storeA().getStoreId()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("门店不存在或无权访问");
        assertThatThrownBy(() -> reportContext.summary(scope.storeManagerB(), scope.storeA().getStoreId()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("门店不存在或无权访问");
    }

    @Test
    void enterpriseScopeRejectsForeignCompanyStore() {
        // ALL_STORES(0) = 不限门店（全部门店），仅企业范围可返回
        assertThat(accessContext.resolveStoreFilter(scope.headFinance(), null))
                .isEqualTo(StoreScope.ALL_STORES);
        assertThatThrownBy(() -> accessContext.resolveStoreFilter(scope.headFinance(), scope.foreignStoreId()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("门店不存在或无权访问");
    }

    @Test
    void employeeWithoutStoreCannotPersist() {
        // FIN-STORE-02：财务账号必须归属门店。员工模型的 store 非空，无门店账号无法落库，
        // 因此不存在「财务无门店」的账号形态；DataScopeContext 的门店校验保留为纵深防御。
        Employee orphan = new Employee();
        orphan.setCompany(scope.company());
        orphan.setRole(EmployeeRole.Finance);
        orphan.setStatus(EmployeeStatus.Active);
        orphan.setName("范围测试无门店财务");
        orphan.setTelNo("19900000001");
        orphan.setJoinedAt(LocalDateTime.now());
        assertThatThrownBy(() -> employeeRepository.saveAndFlush(orphan))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void storeManagerCanBookkeepButOutsiderCannot() {
        // FIN-STORE-06：店长写权限与门店财务一致
        assertThat(accessContext.requireEditor(scope.storeManagerB()).getEmpId())
                .isEqualTo(scope.storeManagerB().getEmpId());
        assertThat(accessContext.requireEditor(scope.storeFinance()).getEmpId())
                .isEqualTo(scope.storeFinance().getEmpId());
        // 非财务角色仍拒绝
        assertThatThrownBy(() -> accessContext.requireEditor(scope.designer()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权执行");
    }

    @Test
    void visibleSpecificationScopesStores() {
        assertThat(receivableRepository.findAll(accessContext.visible(scope.storeFinance(), Receivable.class)))
                .hasSize(1);
        assertThat(receivableRepository.findAll(accessContext.visible(scope.storeManagerB(), Receivable.class)))
                .hasSize(1);
        assertThat(receivableRepository.findAll(accessContext.visible(scope.boss(), Receivable.class)))
                .hasSize(2);
        assertThat(receivableRepository.findAll(accessContext.visible(scope.headFinance(), Receivable.class)))
                .hasSize(2);
    }

    @Test
    void projectLedgerFollowsStoreDimension() {
        assertThat(reportContext.projectReports(scope.storeFinance(), null)).hasSize(1);
        assertThat(reportContext.projectReports(scope.storeManagerB(), null)).hasSize(1);
        assertThat(reportContext.projectReports(scope.headFinance(), null)).hasSize(2);
        assertThat(reportContext.projectReports(scope.headFinance(), scope.storeA().getStoreId())).hasSize(1);
        assertThatThrownBy(() -> reportContext.projectReports(scope.storeFinance(), scope.storeA().getStoreId()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("门店不存在或无权访问");
    }

    @Test
    void projectScopeFollowsStoreForFinance() {
        // 门店财务只能解析本店项目；他店项目按「不存在或无权访问」处理
        assertThat(accessContext.resolveProject(scope.storeFinance(), scope.projectB().getProjectId()).getProjectId())
                .isEqualTo(scope.projectB().getProjectId());
        assertThatThrownBy(() -> accessContext.resolveProject(scope.storeFinance(), scope.projectA().getProjectId()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("项目不存在或无权访问");
    }

    private Scope createScope() {
        long seq = System.nanoTime();
        Company company = company("财务门店范围测试企业", seq);
        Store storeA = store(company, "总店", true);
        Store storeB = store(company, "城北店", false);
        Company otherCompany = company("财务门店范围他企", seq + 7);
        Store foreignStore = store(otherCompany, "他企门店", false);

        Employee boss = employee(company, storeA, EmployeeRole.Boss, "范围测试负责人", seq, 1);
        Employee headFinance = employee(company, storeA, EmployeeRole.Finance, "范围测试总部财务", seq, 2);
        Employee storeFinance = employee(company, storeB, EmployeeRole.Finance, "范围测试门店财务", seq, 3);
        Employee storeManagerB = employee(company, storeB, EmployeeRole.StoreManager, "范围测试店长B", seq, 4);
        Employee designer = employee(company, storeA, EmployeeRole.Designer, "范围测试设计师", seq, 6);

        Project projectA = project(company, storeA, boss, seq, 11, "100.00");
        Project projectB = project(company, storeB, boss, seq, 12, "200.00");
        entityManager.flush();
        return new Scope(company, storeA, storeB, foreignStore.getStoreId(),
                boss, headFinance, storeFinance, storeManagerB, designer, projectA, projectB);
    }

    private Company company(String title, long seq) {
        Company company = new Company();
        company.setTitle(title + Math.abs(seq % 1000));
        company.setName("财务门店范围企业");
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
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setName(name);
        employee.setTelNo("1" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        employee.setJoinedAt(LocalDateTime.now());
        return employeeRepository.save(employee);
    }

    private Project project(Company company, Store store, Employee operator, long seq, int offset,
                            String receivableAmount) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(operator);
        customer.setName("范围测试客户" + offset);
        customer.setTelNo("2" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        customer.setArea(new BigDecimal("120"));
        customer.setAddr("财务门店范围测试地址");
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(operator);
        property.setCustomer(customer);
        property.setPropertyNo("PROP-SCOPE-" + offset + "-" + Math.abs(seq % 100000));
        property.setAddress("财务门店范围测试小区 " + offset + " 栋 101");
        property.setArea(new BigDecimal("120"));
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("QUOTE-SCOPE-" + offset + "-" + Math.abs(seq % 100000));
        quote.setTitle("财务门店范围测试报价");
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
        project.setNo("PROJECT-SCOPE-" + offset + "-" + Math.abs(seq % 100000));
        project.setName("财务门店范围测试项目" + offset);
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
        receivable.setName("首期款" + offset);
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

    private record Scope(Company company, Store storeA, Store storeB, Long foreignStoreId,
                         Employee boss, Employee headFinance, Employee storeFinance, Employee storeManagerB,
                         Employee designer, Project projectA, Project projectB) {
    }
}
