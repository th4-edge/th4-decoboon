package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.customer.configure.JourneyViewMode;
import com.th4.edge.decoboon.customer.context.CustomerJourneyContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerJourney;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.OwnerPreviewResult;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectMember;
import com.th4.edge.decoboon.project.model.ProjectOwnerPreviewLog;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectOwnerPreviewLogRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.service.ownerpreview.info.support.Info;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefinedException;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 员工侧业主视角只读预览的角色、越权、审计、无副作用与字段一致性验收。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:owner-preview;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
class OwnerPreviewInfoAcceptanceTest {

    @Autowired private com.th4.edge.decoboon.project.service.ownerpreview.info.Info provider;
    @Autowired private com.th4.edge.decoboon.project.service.projects.info.Info projectInfoProvider;
    @Autowired private com.th4.edge.decoboon.project.service.stages.page.Page projectStagePageProvider;
    @Autowired private com.th4.edge.decoboon.project.service.members.list.List projectMemberListProvider;
    @Autowired private com.th4.edge.decoboon.project.service.tasks.page.Page projectTaskPageProvider;
    @Autowired private CustomerJourneyContext journeyContext;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectStageRepository stageRepository;
    @Autowired private AcceptanceRepository acceptanceRepository;
    @Autowired private ReceivableRepository receivableRepository;
    @Autowired private CustomerAccessRepository customerAccessRepository;
    @Autowired private ProjectOwnerPreviewLogRepository auditRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void fiveAllowedRolesCanOpenPreviewAndAreAudited() {
        Fixture fixture = fixture("allowed");

        for (EmployeeRole role : List.of(EmployeeRole.Boss, EmployeeRole.StoreManager,
                EmployeeRole.TeamLeader, EmployeeRole.Designer, EmployeeRole.Pm)) {
            Employee employee = fixture.employee(role);
            CustomerJourney journey = call(employee, fixture.project().getProjectId());
            assertThat(journey.getActions()).isEmpty();
            assertThat(journey.getPreviewMeta()).isNotNull();
            assertThat(journey.getPreviewMeta().getReadOnly()).isTrue();
            assertThat(journey.getPreviewMeta().getWatermark())
                    .startsWith("业主视角预览 · 仅内部使用 · ")
                    .contains(employee.getName())
                    .contains("工号" + employee.getEmpId());
        }

        List<ProjectOwnerPreviewLog> logs = auditRepository
                .findByCompany_ComIdAndRequestedProjectIdOrderByLogIdAsc(
                        fixture.company().getComId(), fixture.project().getProjectId());
        assertThat(logs).hasSize(5);
        assertThat(logs).extracting(ProjectOwnerPreviewLog::getResult)
                .containsOnly(OwnerPreviewResult.Success);
        assertThat(logs).extracting(ProjectOwnerPreviewLog::getEmployeeRole)
                .containsExactlyInAnyOrder(EmployeeRole.Boss, EmployeeRole.StoreManager,
                        EmployeeRole.TeamLeader, EmployeeRole.Designer, EmployeeRole.Pm);
        assertThat(logs).allSatisfy(log -> {
            assertThat(log.getCompany().getComId()).isEqualTo(fixture.company().getComId());
            assertThat(log.getProject().getProjectId()).isEqualTo(fixture.project().getProjectId());
            assertThat(log.getSource().name()).isEqualTo("App");
            assertThat(log.getOpenedAt()).isNotNull();
        });
    }

    @Test
    void projectDetailEntryProvidersWorkWithOsivDisabled() {
        Fixture fixture = fixture("osiv-false");
        Employee boss = fixture.boss();

        var projectDetailTarget = (com.th4.edge.decoboon.project.service.projects.info.support.Info)
                target(projectInfoProvider, boss, fixture.project().getProjectId());
        Project detail = transactionTemplate.execute(status -> projectDetailTarget.call(new Project()));
        assertThat(detail).isNotNull();
        assertThat(detail.getCustomer().getName()).isNotBlank();
        assertThat(detail.getCustomer().getOwner().getName()).isNotBlank();
        assertThat(detail.getProperty().getCustomer().getName()).isNotBlank();
        assertThat(detail.getQuote().getCreatedBy().getName()).isNotBlank();
        assertThat(detail.getDesigner().getName()).isNotBlank();
        assertThat(detail.getManager().getName()).isNotBlank();

        var projectStageTarget = (com.th4.edge.decoboon.project.service.stages.page.support.Page)
                target(projectStagePageProvider, boss, fixture.project().getProjectId());
        var stagePage = transactionTemplate.execute(status -> projectStageTarget.call(new ProjectStage()));
        assertThat(stagePage).isNotNull();
        List<ProjectStage> stages = stagePage.getContent();
        assertThat(stages).isNotEmpty();
        assertThat(stages).allSatisfy(stage -> {
            assertThat(stage.getProject().getName()).isEqualTo(fixture.project().getName());
            assertThat(stage.getProject().getStore().getName()).isEqualTo("主店");
        });

        var projectMemberTarget = (com.th4.edge.decoboon.project.service.members.list.support.List)
                target(projectMemberListProvider, boss, fixture.project().getProjectId());
        List<ProjectMember> members = transactionTemplate.execute(status -> projectMemberTarget.call(new ProjectMember()));
        assertThat(members).isNotNull();
        assertThat(members).isNotEmpty();
        assertThat(members).allSatisfy(member -> {
            assertThat(member.getProject().getName()).isEqualTo(fixture.project().getName());
            assertThat(member.getProject().getProperty().getAddress())
                    .isEqualTo(fixture.project().getProperty().getAddress());
        });

        var projectTaskTarget = (com.th4.edge.decoboon.project.service.tasks.page.support.Page)
                target(projectTaskPageProvider, boss, fixture.project().getProjectId());
        var taskPage = transactionTemplate.execute(status -> projectTaskTarget.call(new ProjectTask()));
        assertThat(taskPage).isNotNull();
    }

    @Test
    void marketerAndFinanceAreForbiddenBeforeProjectReadAndAudited() {
        Fixture fixture = fixture("forbidden");

        for (EmployeeRole role : List.of(EmployeeRole.Marketer, EmployeeRole.Finance)) {
            assertDefined(() -> call(fixture.employee(role), fixture.project().getProjectId()), "403");
        }

        List<ProjectOwnerPreviewLog> logs = auditRepository
                .findByCompany_ComIdAndRequestedProjectIdOrderByLogIdAsc(
                        fixture.company().getComId(), fixture.project().getProjectId());
        assertThat(logs).hasSize(2);
        assertThat(logs).extracting(ProjectOwnerPreviewLog::getEmployeeRole)
                .containsExactlyInAnyOrder(EmployeeRole.Marketer, EmployeeRole.Finance);
        assertThat(logs).extracting(ProjectOwnerPreviewLog::getResult)
                .containsOnly(OwnerPreviewResult.Forbidden);
        assertThat(logs).allSatisfy(log -> assertThat(log.getProject()).isNull());
    }

    @Test
    void crossCompanyNonMemberWrongStoreAndWrongTeamAreRejectedAndAudited() {
        Fixture fixture = fixture("scope");
        Project otherCompanyProject = otherCompanyProject("other-company");

        assertDefined(() -> call(fixture.employee(EmployeeRole.Boss), otherCompanyProject.getProjectId()), "404");
        assertDefined(() -> call(fixture.nonMemberDesigner(), fixture.project().getProjectId()), "404");
        assertDefined(() -> call(fixture.otherStoreManager(), fixture.project().getProjectId()), "404");
        assertDefined(() -> call(fixture.otherTeamLeader(), fixture.project().getProjectId()), "404");

        assertThat(auditRepository.findByCompany_ComIdAndRequestedProjectIdOrderByLogIdAsc(
                fixture.company().getComId(), otherCompanyProject.getProjectId())).hasSize(1);
        List<ProjectOwnerPreviewLog> projectLogs = auditRepository
                .findByCompany_ComIdAndRequestedProjectIdOrderByLogIdAsc(
                        fixture.company().getComId(), fixture.project().getProjectId());
        assertThat(projectLogs).hasSize(3);
        assertThat(projectLogs).extracting(ProjectOwnerPreviewLog::getResult)
                .containsOnly(OwnerPreviewResult.NotFound);
    }

    @Test
    void previewHasNoBusinessSideEffectsAndMatchesOwnerProjection() {
        Fixture fixture = fixture("consistency");
        Project project = projectRepository.findById(fixture.project().getProjectId()).orElseThrow();

        long projectsBefore = projectRepository.count();
        long stagesBefore = stageRepository.count();
        long acceptancesBefore = acceptanceRepository.count();
        long receivablesBefore = receivableRepository.count();
        long accessBefore = customerAccessRepository.count();
        long auditsBefore = auditRepository.count();
        CustomerJourney owner = transactionTemplate.execute(status -> journeyContext.build(
                projectRepository.findById(project.getProjectId()).orElseThrow(), JourneyViewMode.Owner));

        CustomerJourney preview = call(fixture.employee(EmployeeRole.Boss), project.getProjectId());

        assertThat(projectRepository.count()).isEqualTo(projectsBefore);
        assertThat(stageRepository.count()).isEqualTo(stagesBefore);
        assertThat(acceptanceRepository.count()).isEqualTo(acceptancesBefore);
        assertThat(receivableRepository.count()).isEqualTo(receivablesBefore);
        assertThat(customerAccessRepository.count()).isEqualTo(accessBefore);
        assertThat(auditRepository.count()).isEqualTo(auditsBefore + 1);
        Project after = projectRepository.findById(project.getProjectId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(project.getStatus());
        assertThat(after.getProgress()).isEqualTo(project.getProgress());

        assertThat(owner.getPreviewMeta()).isNull();
        assertThat(preview.getPreviewMeta()).isNotNull();
        assertThat(preview.getProjectId()).isEqualTo(owner.getProjectId());
        assertThat(preview.getProjectName()).isEqualTo(owner.getProjectName());
        assertThat(preview.getProjectStatus()).isEqualTo(owner.getProjectStatus());
        assertThat(preview.getProjectStatusName()).isEqualTo(owner.getProjectStatusName());
        assertThat(preview.getProgress()).isEqualTo(owner.getProgress());
        assertThat(preview.getCurrentStageName()).isEqualTo(owner.getCurrentStageName());
        assertThat(preview.getPropertyAddress()).isEqualTo(owner.getPropertyAddress());
        assertThat(preview.getVendor()).usingRecursiveComparison().isEqualTo(owner.getVendor());
        assertThat(preview.getTeam()).usingRecursiveComparison().isEqualTo(owner.getTeam());
        assertThat(preview.getSections()).extracting(CustomerJourney.Section::getKey)
                .containsExactlyElementsOf(owner.getSections().stream()
                        .map(CustomerJourney.Section::getKey)
                        .filter(key -> !"feedback".equals(key))
                        .toList());
        for (CustomerJourney.Section previewSection : preview.getSections()) {
            CustomerJourney.Section ownerSection = owner.getSections().stream()
                    .filter(section -> previewSection.getKey().equals(section.getKey()))
                    .findFirst().orElseThrow();
            assertThat(previewSection).usingRecursiveComparison().isEqualTo(ownerSection);
        }
    }

    private Object target(Object provider, Employee employee, Long projectId) {
        Object target = AopTestUtils.getTargetObject(provider);
        ReflectionTestUtils.setField(target, "employee", employee);
        ReflectionTestUtils.setField(target, "projectId", projectId);
        return target;
    }

    private CustomerJourney call(Employee employee, Long projectId) {
        com.th4.edge.decoboon.project.service.ownerpreview.info.Info target = AopTestUtils.getTargetObject(provider);
        ReflectionTestUtils.setField(target, "employee", employee);
        ReflectionTestUtils.setField(target, "projectId", projectId);
        CustomerJourney journey = transactionTemplate.execute(status -> target.call(new Project()));
        return java.util.Objects.requireNonNull(journey);
    }

    private void assertDefined(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThatThrownBy(callable).isInstanceOf(DefinedException.class)
                .satisfies(ex -> assertThat(((DefinedException) ex).getErrorCode().getCode()).isEqualTo(code));
    }

    private Fixture fixture(String prefix) {
        String key = prefix + "-" + Long.toHexString(System.nanoTime());
        Company company = company("预览测试企业");
        Store store = store(company, "主店");
        Store otherStore = store(company, "他店");
        Employee boss = employee(company, store, EmployeeRole.Boss, "老板");
        Team team = team(company, store, boss, "本团队");
        Team otherTeam = team(company, store, boss, "他团队");
        Employee storeManager = employee(company, store, EmployeeRole.StoreManager, "店长");
        Employee teamLeader = employee(company, store, EmployeeRole.TeamLeader, "团队负责人");
        teamLeader.setTeam(team);
        teamLeader = employeeRepository.save(teamLeader);
        Employee designer = employee(company, store, EmployeeRole.Designer, "设计师");
        Employee pm = employee(company, store, EmployeeRole.Pm, "项目经理");
        Employee marketer = employee(company, store, EmployeeRole.Marketer, "业务员");
        Employee finance = employee(company, store, EmployeeRole.Finance, "财务");
        Employee nonMemberDesigner = employee(company, store, EmployeeRole.Designer, "非成员设计师");
        Employee otherStoreManager = employee(company, otherStore, EmployeeRole.StoreManager, "他店店长");
        Employee otherTeamLeader = employee(company, store, EmployeeRole.TeamLeader, "他团队负责人");
        otherTeamLeader.setTeam(otherTeam);
        otherTeamLeader = employeeRepository.save(otherTeamLeader);

        Customer customer = customer(company, store, team, marketer, key);
        Property property = property(company, store, customer, designer, key);
        Quote quote = quote(company, store, customer, property, designer, key);
        Project project = project(company, store, team, customer, property, quote, marketer, designer, pm, key);
        stage(company, project, property);

        return new Fixture(company, project, boss, storeManager, teamLeader, designer, pm, marketer,
                finance, nonMemberDesigner, otherStoreManager, otherTeamLeader);
    }

    private Project otherCompanyProject(String prefix) {
        String key = prefix + "-" + Long.toHexString(System.nanoTime());
        Company company = company("其他企业");
        Store store = store(company, "其他门店");
        Employee boss = employee(company, store, EmployeeRole.Boss, "其他老板");
        Team team = team(company, store, boss, "其他团队");
        Customer customer = customer(company, store, team, boss, key);
        Property property = property(company, store, customer, boss, key);
        Quote quote = quote(company, store, customer, property, boss, key);
        return project(company, store, team, customer, property, quote, boss, boss, boss, key);
    }

    private Company company(String title) {
        Company company = new Company();
        company.setTitle(title);
        company.setName("C" + Long.toHexString(System.nanoTime()));
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company, String name) {
        Store store = new Store();
        store.setCompany(company);
        store.setName(name);
        store.setDefaultStore(true);
        return storeRepository.save(store);
    }

    private Team team(Company company, Store store, Employee leader, String name) {
        Team team = new Team();
        team.setCompany(company);
        team.setStore(store);
        team.setLeader(leader);
        team.setName(name);
        return teamRepository.save(team);
    }

    private Employee employee(Company company, Store store, EmployeeRole role, String name) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setName(name);
        employee.setTelNo("1" + Math.abs(System.nanoTime() % 1_000_000_000L));
        return employeeRepository.save(employee);
    }

    private Customer customer(Company company, Store store, Team team, Employee owner, String key) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setTeam(team);
        customer.setOwner(owner);
        customer.setName("业主" + key);
        customer.setTelNo("2" + Math.abs(System.nanoTime() % 1_000_000_000L));
        customer.setCommunity("预览小区");
        customer.setAddr("预览小区 1-101");
        customer.setArea(new BigDecimal("100"));
        return customerRepository.save(customer);
    }

    private Property property(Company company, Store store, Customer customer, Employee designer, String key) {
        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(designer);
        property.setCustomer(customer);
        property.setPropertyNo("PP-" + key);
        property.setCommunity(customer.getCommunity());
        property.setBuilding("1");
        property.setRoomNo("101");
        property.setAddress(customer.getAddr());
        property.setArea(customer.getArea());
        property.setLayout("3室2厅");
        property.setStatus(PropertyStatus.Serving);
        return propertyRepository.save(property);
    }

    private Quote quote(Company company, Store store, Customer customer, Property property,
                        Employee designer, String key) {
        Quote quote = new Quote();
        quote.setNo("QO-" + key);
        quote.setTitle("预览报价");
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(designer);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(property.getArea());
        quote.setSubtotal(new BigDecimal("100000"));
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setAmount(new BigDecimal("100000"));
        quote.setCost(new BigDecimal("80000"));
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(QuoteStatus.Accepted);
        return quoteRepository.save(quote);
    }

    private Project project(Company company, Store store, Team team, Customer customer, Property property,
                            Quote quote, Employee owner, Employee designer, Employee manager, String key) {
        Project project = new Project();
        project.setNo("PJO-" + key);
        project.setName("业主视角预览项目");
        project.setCompany(company);
        project.setStore(store);
        project.setTeam(team);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setOwner(owner);
        project.setDesigner(designer);
        project.setManager(manager);
        project.setSiteAddr(property.getAddress());
        project.setArea(property.getArea());
        project.setContractAmount(new BigDecimal("100000"));
        project.setCost(new BigDecimal("80000"));
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(38);
        project.setStartDate(LocalDate.of(2026, 10, 1));
        project.setPlanEndDate(LocalDate.of(2027, 1, 31));
        return projectRepository.save(project);
    }

    private ProjectStage stage(Company company, Project project, Property property) {
        ProjectStage stage = new ProjectStage();
        stage.setCompany(company);
        stage.setProject(project);
        stage.setProperty(property);
        stage.setName("水电施工");
        stage.setStageCode("WATER_ELECTRIC");
        stage.setSort(2);
        stage.setWeight(new BigDecimal("50"));
        stage.setStatus(StageStatus.Doing);
        stage.setProgress(40);
        stage.setPlannedStart(LocalDate.of(2026, 10, 10));
        stage.setPlannedEnd(LocalDate.of(2026, 10, 31));
        return stageRepository.save(stage);
    }

    private record Fixture(Company company, Project project, Employee boss, Employee storeManager,
                           Employee teamLeader, Employee designer, Employee pm, Employee marketer,
                           Employee finance, Employee nonMemberDesigner, Employee otherStoreManager,
                           Employee otherTeamLeader) {
        Employee employee(EmployeeRole role) {
            return switch (role) {
                case Boss -> boss;
                case StoreManager -> storeManager;
                case TeamLeader -> teamLeader;
                case Designer -> designer;
                case Pm -> pm;
                case Marketer -> marketer;
                case Finance -> finance;
            };
        }
    }
}
