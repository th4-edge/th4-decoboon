package com.th4.edge.decoboon.common.access;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.context.StandardRequestContext;
import com.th4.edge.framework.provider.criteria.CriteriaQueryAware;
import com.th4.edge.framework.provider.RequestContextHolder;
import com.th4.edge.framework.provider.security.ProviderSecurityConfigure;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.provider.security.SecurityManager;
import com.th4.edge.framework.provider.security.Token;
import com.th4.edge.framework.provider.security.jwt.JwtToken;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.access.filter.AccessDataPermissionSaveRequestFilter;
import com.th4.edge.tenant.user.access.filter.AccessDataPermissionUpdateRequestFilter;
import com.th4.edge.tenant.user.access.filter.DataScopeRequestFilter;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.tenant.user.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Proxy;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 四级数据范围拦截与 Specification 查询集成测试。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:datascope;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
@Import(DataScopeRequestFilterIntegrationTest.RedissonTestConfiguration.class)
@Transactional
class DataScopeRequestFilterIntegrationTest {

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private TeamRepository teamRepository;
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
    private ProjectAccessContext projectAccessContext;
    @Autowired
    private DataScopeRequestFilter filter;
    @Autowired
    private AccessDataPermissionSaveRequestFilter saveFilter;
    @Autowired
    private AccessDataPermissionUpdateRequestFilter updateFilter;
    @Autowired
    private DataScopeContext dataScopeContext;
    @Autowired
    private SecurityManager securityManager;
    @Autowired
    private ProviderSecurityConfigure securityConfigure;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldApplyCompanyStoreTeamAndSelfScopes() {
        Fixture fixture = createFixture();

        assertNames(fixture.boss(), Set.of(
                "老板客户", "店长客户", "团队负责人客户", "团队成员客户", "其他门店客户"));
        assertNames(fixture.storeManager(), Set.of(
                "老板客户", "店长客户", "团队负责人客户", "团队成员客户"));
        assertThatThrownBy(() -> assertNames(fixture.teamLeader(), Set.of()))
                .hasMessageContaining("团队负责人角色已冻结");
        assertNames(fixture.marketer(), Set.of("团队成员客户"));

        assertThatThrownBy(() -> dataScopeContext.resolve(fixture.teamLeader()))
                .hasMessageContaining("团队负责人角色已冻结");
    }

    @Test
    void shouldGuardGenericModelServiceCrudByDataPermission() {
        Fixture fixture = createFixture();
        Customer create = new Customer();
        create.setName("泛化保存客户");
        create.setTelNo("19900000999");
        Customer created = execute("service/save", fixture.marketer(), create);
        assertThat(created.getCompany().getComId()).isEqualTo(fixture.boss().getCompany().getComId());
        assertThat(created.getStore().getStoreId()).isEqualTo(fixture.marketer().getStore().getStoreId());
        assertThat(created.getTeam().getTeamId()).isEqualTo(fixture.marketer().getTeam().getTeamId());
        assertThat(created.getOwner().getEmpId()).isEqualTo(fixture.marketer().getEmpId());

        Customer bossCreate = new Customer();
        bossCreate.setName("BOSS 分配客户");
        bossCreate.setTelNo("19900000998");
        Employee requestedOwner = new Employee();
        requestedOwner.setEmpId(fixture.teamLeader().getEmpId());
        bossCreate.setOwner(requestedOwner);
        Customer assigned = execute("service/save", fixture.boss(), bossCreate);
        assertThat(assigned.getOwner().getEmpId()).isEqualTo(fixture.teamLeader().getEmpId());
        assertThat(assigned.getStore().getStoreId()).isEqualTo(fixture.teamLeader().getStore().getStoreId());
        assertThat(assigned.getTeam().getTeamId()).isEqualTo(fixture.teamLeader().getTeam().getTeamId());

        Customer teamCustomer = customerByName("团队成员客户");
        assertThatThrownBy(() -> execute("service/info", fixture.teamLeader(), request(teamCustomer)))
                .hasMessageContaining("团队负责人角色已冻结");
        Customer otherStoreCustomer = customerByName("其他门店客户");
        assertThatThrownBy(() -> execute("service/info",
                fixture.storeManager(), request(otherStoreCustomer)))
                .hasMessageContaining("数据不存在或无权访问");

        Customer update = new Customer();
        update.setCustId(teamCustomer.getCustId());
        update.setName("团队客户已更新");
        update.setOwner(new Employee());
        update.getOwner().setEmpId(fixture.boss().getEmpId());
        Customer updated = execute("service/update", fixture.marketer(), update);
        assertThat(updated.getName()).isEqualTo("团队客户已更新");
        assertThat(updated.getOwner().getEmpId()).isEqualTo(fixture.marketer().getEmpId());

        assertThatThrownBy(() -> execute("service/delete",
                fixture.storeManager(), request(otherStoreCustomer)))
                .hasMessageContaining("数据不存在或无权访问");

        execute("service/delete", fixture.marketer(), request(teamCustomer));
        entityManager.flush();
        assertThat(entityManager.find(Customer.class, teamCustomer.getCustId())).isNull();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldOverrideDataPermissionOwnershipOnModelSave() {
        Fixture fixture = createFixture();
        Provider<Object, ?> saveProvider =
                (Provider<Object, ?>) applicationContext.getBean("service/save");

        Customer memberRequest = new Customer();
        Employee forgedOwner = new Employee();
        forgedOwner.setEmpId(fixture.boss().getEmpId());
        memberRequest.setOwner(forgedOwner);
        Customer memberPrepared = (Customer) saveFilter.prepare(
                saveProvider, memberRequest, requestContext(fixture.marketer()));
        assertThat(memberPrepared.getCompany().getComId())
                .isEqualTo(fixture.marketer().getCompany().getComId());
        assertThat(memberPrepared.getOwner().getEmpId())
                .isEqualTo(fixture.marketer().getEmpId());
        assertThat(memberPrepared.getStore().getStoreId())
                .isEqualTo(fixture.marketer().getStore().getStoreId());
        assertThat(memberPrepared.getTeam().getTeamId())
                .isEqualTo(fixture.marketer().getTeam().getTeamId());

        Customer bossRequest = new Customer();
        Employee requestedOwner = new Employee();
        requestedOwner.setEmpId(fixture.teamLeader().getEmpId());
        bossRequest.setOwner(requestedOwner);
        Customer bossPrepared = (Customer) saveFilter.prepare(
                saveProvider, bossRequest, requestContext(fixture.boss()));
        assertThat(bossPrepared.getOwner().getEmpId())
                .isEqualTo(fixture.teamLeader().getEmpId());
        assertThat(bossPrepared.getStore().getStoreId())
                .isEqualTo(fixture.teamLeader().getStore().getStoreId());
        assertThat(bossPrepared.getTeam().getTeamId())
                .isEqualTo(fixture.teamLeader().getTeam().getTeamId());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldProtectDataPermissionOwnershipOnModelUpdate() {
        Fixture fixture = createFixture();
        Provider<Object, ?> updateProvider =
                (Provider<Object, ?>) applicationContext.getBean("service/update");
        Customer teamCustomer = customerByName("团队成员客户");

        Customer memberUpdate = new Customer();
        memberUpdate.setCustId(teamCustomer.getCustId());
        memberUpdate.setName("成员修改客户");
        Employee forgedOwner = new Employee();
        forgedOwner.setEmpId(fixture.boss().getEmpId());
        memberUpdate.setOwner(forgedOwner);
        Customer memberPrepared = (Customer) updateFilter.prepare(
                updateProvider, memberUpdate, requestContext(fixture.marketer()));
        assertThat(memberPrepared.getName()).isEqualTo("成员修改客户");
        assertThat(memberPrepared.getOwner().getEmpId())
                .isEqualTo(fixture.marketer().getEmpId());
        assertThat(memberPrepared.getStore().getStoreId())
                .isEqualTo(fixture.marketer().getStore().getStoreId());

        Customer otherStoreCustomer = customerByName("其他门店客户");
        Customer invalidUpdate = new Customer();
        invalidUpdate.setCustId(otherStoreCustomer.getCustId());
        invalidUpdate.setName("越权修改");
        assertThatThrownBy(() -> updateFilter.prepare(
                updateProvider, invalidUpdate, requestContext(fixture.storeManager())))
                .hasMessageContaining("数据不存在或无权访问");

        Customer bossUpdate = new Customer();
        bossUpdate.setCustId(teamCustomer.getCustId());
        Employee requestedOwner = new Employee();
        requestedOwner.setEmpId(fixture.teamLeader().getEmpId());
        bossUpdate.setOwner(requestedOwner);
        Customer bossPrepared = (Customer) updateFilter.prepare(
                updateProvider, bossUpdate, requestContext(fixture.boss()));
        assertThat(bossPrepared.getOwner().getEmpId())
                .isEqualTo(fixture.teamLeader().getEmpId());
        assertThat(bossPrepared.getTeam().getTeamId())
                .isEqualTo(fixture.teamLeader().getTeam().getTeamId());
    }

    @Test
    void shouldGuardGenericModelServicePageByDataPermission() {
        Fixture fixture = createFixture();

        assertThat(executePage(fixture.boss()).getContent())
                .extracting(Customer::getName)
                .containsExactlyInAnyOrder(
                        "老板客户", "店长客户", "团队负责人客户", "团队成员客户", "其他门店客户");
        assertThat(executePage(fixture.storeManager()).getContent())
                .extracting(Customer::getName)
                .containsExactlyInAnyOrder(
                        "老板客户", "店长客户", "团队负责人客户", "团队成员客户");
        assertThatThrownBy(() -> executePage(fixture.teamLeader()))
                .hasMessageContaining("团队负责人角色已冻结");
        assertThat(executePage(fixture.marketer()).getContent())
                .extracting(Customer::getName)
                .containsExactly("团队成员客户");
    }

    @Test
    void shouldApplyProjectOwnershipAndResolveCrossScopeDetailAsNotFound() {
        Fixture fixture = createFixture();
        Project bossProject = project(customerByName("老板客户"), "老板项目");
        Project storeProject = project(customerByName("店长客户"), "店长项目");
        Project teamProject = project(customerByName("团队负责人客户"), "团队项目");
        Project selfProject = project(customerByName("团队成员客户"), "团队成员项目");
        Project otherStoreProject = project(customerByName("其他门店客户"), "其他门店项目");

        assertThat(projectRepository.findAll(DataScopeSpecification.visible(
                Project.class, dataScopeContext.requireScope(fixture.boss()))))
                .extracting(Project::getName)
                .containsExactlyInAnyOrder("老板项目", "店长项目", "团队项目", "团队成员项目", "其他门店项目");
        assertThat(projectRepository.findAll(DataScopeSpecification.visible(
                Project.class, dataScopeContext.requireScope(fixture.storeManager()))))
                .extracting(Project::getName)
                .containsExactlyInAnyOrder("老板项目", "店长项目", "团队项目", "团队成员项目");
        assertThatThrownBy(() -> dataScopeContext.requireScope(fixture.teamLeader()))
                .hasMessageContaining("团队负责人角色已冻结");
        assertThat(projectRepository.findAll(DataScopeSpecification.visible(
                Project.class, dataScopeContext.requireScope(fixture.marketer()))))
                .extracting(Project::getName)
                .containsExactly("团队成员项目");

        assertThat(projectAccessContext.requireProject(storeProject.getProjectId(), fixture.storeManager()))
                .isNotNull();
        assertThat(projectAccessContext.requireProject(selfProject.getProjectId(), fixture.marketer()).getProjectId())
                .isEqualTo(selfProject.getProjectId());
        assertThatThrownBy(() -> projectAccessContext.requireProject(
                otherStoreProject.getProjectId(), fixture.storeManager()))
                .hasMessageContaining("项目不存在或无权访问");
        assertThatThrownBy(() -> projectAccessContext.requireProject(
                storeProject.getProjectId(), fixture.marketer()))
                .hasMessageContaining("项目不存在或无权访问");
    }

    private void assertNames(Employee employee, Set<String> expected) {
        StandardRequestContext requestContext = new StandardRequestContext();
        Token token = securityManager.sign(employee, requestContext);
        requestContext.setHeader(securityConfigure.getTokenKey(), ((JwtToken) token).getToken());

        TestCriteriaProvider provider = new TestCriteriaProvider(entityManager);
        assertThat(filter.support(provider, new Customer())).isTrue();
        filter.prepare(provider, new Customer(), requestContext);

        List<?> rows = entityManager.createQuery(provider.getCriteriaQuery()).getResultList();
        Set<String> names = rows.stream()
                .map(Customer.class::cast)
                .map(Customer::getName)
                .collect(Collectors.toSet());
        assertThat(names).containsExactlyInAnyOrderElementsOf(expected);
    }

    private Customer execute(String serviceName, Employee employee, Customer request) {
        StandardRequestContext requestContext = requestContext(employee);
        RequestContextHolder.setRequestContext(requestContext);
        try {
            @SuppressWarnings("unchecked")
            ModelService<Customer, Customer> service =
                    (ModelService<Customer, Customer>) applicationContext.getBean(serviceName);
            return service.call(request);
        } finally {
            RequestContextHolder.removeRequestContext();
        }
    }

    private Page<Customer> executePage(Employee employee) {
        StandardRequestContext requestContext = requestContext(employee);
        RequestContextHolder.setRequestContext(requestContext);
        try {
            @SuppressWarnings("unchecked")
            ModelService<Customer, Page<Customer>> service =
                    (ModelService<Customer, Page<Customer>>) applicationContext.getBean("service/page");
            return service.call(new Customer());
        } finally {
            RequestContextHolder.removeRequestContext();
        }
    }

    private Customer request(Customer customer) {
        Customer request = new Customer();
        request.setCustId(customer.getCustId());
        return request;
    }

    private Customer customerByName(String name) {
        return customerRepository.findAll().stream()
                .filter(customer -> name.equals(customer.getName()))
                .findFirst()
                .orElseThrow();
    }

    private StandardRequestContext requestContext(Employee employee) {
        StandardRequestContext requestContext = new StandardRequestContext();
        Token token = securityManager.sign(employee, requestContext);
        requestContext.setHeader(securityConfigure.getTokenKey(), ((JwtToken) token).getToken());
        return requestContext;
    }

    private Fixture createFixture() {
        Company company = new Company();
        company.setComId(900001L);
        company.setTitle("四级权限测试企业");
        company.setName("权限企业");
        company.setSize(Size.A);
        company = companyRepository.save(company);

        Store storeA = store(900011L, company, "A 店");
        Store storeB = store(900012L, company, "B 店");

        Employee boss = employee(900021L, company, storeA, null,
                EmployeeRole.Boss, "权限老板");
        boss = employeeRepository.save(boss);

        Team teamA = new Team();
        teamA.setTeamId(900031L);
        teamA.setCompany(company);
        teamA.setStore(storeA);
        teamA.setName("A 团队");
        teamA.setLeader(boss);

        Employee storeManager = employee(900022L, company, storeA, null,
                EmployeeRole.StoreManager, "A 店店长");
        storeManager = employeeRepository.save(storeManager);

        Employee teamLeader = employee(900023L, company, storeA, null,
                EmployeeRole.TeamLeader, "A 团队负责人");
        teamLeader = employeeRepository.save(teamLeader);

        teamA.setLeader(teamLeader);
        teamA = teamRepository.save(teamA);

        teamLeader.setTeam(teamA);
        teamLeader = employeeRepository.save(teamLeader);

        Employee marketer = employee(900024L, company, storeA, teamA,
                EmployeeRole.Marketer, "A 团队成员");
        marketer = employeeRepository.save(marketer);

        Employee otherMarketer = employee(900025L, company, storeB, null,
                EmployeeRole.Marketer, "B 店成员");
        otherMarketer = employeeRepository.save(otherMarketer);

        customer(company, storeA, null, boss, "老板客户");
        customer(company, storeA, null, storeManager, "店长客户");
        customer(company, storeA, teamA, teamLeader, "团队负责人客户");
        customer(company, storeA, teamA, marketer, "团队成员客户");
        customer(company, storeB, null, otherMarketer, "其他门店客户");

        Company otherCompany = new Company();
        otherCompany.setComId(900002L);
        otherCompany.setTitle("其他企业");
        otherCompany.setName("其他企业");
        otherCompany.setSize(Size.A);
        otherCompany = companyRepository.save(otherCompany);
        Store otherStore = store(900013L, otherCompany, "其他门店");
        Employee otherBoss = employee(900026L, otherCompany, otherStore, null,
                EmployeeRole.Boss, "其他企业老板");
        otherBoss = employeeRepository.save(otherBoss);
        customer(otherCompany, otherStore, null, otherBoss, "其他企业客户");
        entityManager.flush();

        return new Fixture(boss, storeManager, teamLeader, marketer);
    }

    private Store store(Long id, Company company, String name) {
        Store store = new Store();
        store.setStoreId(id);
        store.setCompany(company);
        store.setName(name);
        return storeRepository.save(store);
    }

    private Employee employee(Long id, Company company, Store store, Team team,
                              EmployeeRole role, String name) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setCompany(company);
        employee.setStore(store);
        employee.setTeam(team);
        employee.setRole(role);
        employee.setName(name);
        employee.setTelNo(String.valueOf(id));
        return employee;
    }

    private Project project(Customer customer, String name) {
        long seq = System.nanoTime();
        Property property = new Property();
        property.setCompany(customer.getCompany());
        property.setStore(customer.getStore());
        property.setTeam(customer.getTeam());
        property.setOwner(customer.getOwner());
        property.setCustomer(customer);
        property.setPropertyNo("P-" + seq);
        property.setAddress(name + "地址");
        property.setArea(new BigDecimal("100"));
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("Q-" + seq);
        quote.setTitle(name + "报价");
        quote.setCompany(customer.getCompany());
        quote.setStore(customer.getStore());
        quote.setTeam(customer.getTeam());
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(customer.getOwner());
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(new BigDecimal("100"));
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
        project.setNo("PJ-" + seq);
        project.setName(name);
        project.setCompany(customer.getCompany());
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setDesigner(customer.getOwner());
        project.setManager(customer.getOwner());
        project.setSupport(customer.getOwner());
        project.setStore(customer.getStore());
        project.setTeam(customer.getTeam());
        project.setOwner(customer.getOwner());
        project.setSiteAddr(property.getAddress());
        project.setArea(new BigDecimal("100"));
        project.setContractAmount(new BigDecimal("100000"));
        project.setCost(new BigDecimal("80000"));
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(10);
        project.setStartDate(LocalDate.now());
        project.setPlanEndDate(LocalDate.now().plusDays(90));
        return projectRepository.save(project);
    }

    private void customer(Company company, Store store, Team team,
                          Employee owner, String name) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setTeam(team);
        customer.setOwner(owner);
        customer.setName(name);
        customer.setTelNo(String.valueOf(900100L + customerRepository.count()));
        customerRepository.save(customer);
    }

    private record Fixture(Employee boss, Employee storeManager,
                           Employee teamLeader, Employee marketer) {
    }

    private static final class TestCriteriaProvider
            implements Provider<Object, List<?>>, Security, CriteriaQueryAware {

        private final CriteriaQuery<Object> criteriaQuery;

        private TestCriteriaProvider(EntityManager entityManager) {
            this.criteriaQuery = entityManager.getCriteriaBuilder().createQuery();
            Root<Customer> root = criteriaQuery.from(Customer.class);
            criteriaQuery.select(root);
        }

        @Override
        public List<?> call(Object request) {
            return List.of();
        }

        @Override
        public void setCriteriaQuery(CriteriaQuery<Object> criteriaQuery) {
            throw new UnsupportedOperationException("测试 Provider 不允许替换 CriteriaQuery");
        }

        @Override
        public CriteriaQuery<Object> getCriteriaQuery() {
            return criteriaQuery;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RedissonTestConfiguration {

        @Bean
        @Primary
        RedissonClient testRedissonClient() {
            return (RedissonClient) Proxy.newProxyInstance(
                    RedissonClient.class.getClassLoader(),
                    new Class<?>[]{RedissonClient.class},
                    (proxy, method, arguments) -> {
                        if (method.getReturnType() == boolean.class) {
                            return false;
                        }
                        if (method.getReturnType() == int.class) {
                            return 0;
                        }
                        if (method.getReturnType() == long.class) {
                            return 0L;
                        }
                        return null;
                    });
        }
    }
}
