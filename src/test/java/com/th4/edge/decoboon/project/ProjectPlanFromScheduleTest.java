package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.plan.configure.PlanSnapshotStatus;
import com.th4.edge.decoboon.plan.configure.StageAcceptanceMode;
import com.th4.edge.decoboon.plan.model.ContractConstructionPlan;
import com.th4.edge.decoboon.plan.model.ContractConstructionStage;
import com.th4.edge.decoboon.plan.repository.ContractConstructionPlanRepository;
import com.th4.edge.decoboon.plan.repository.ContractConstructionStageRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 项目计划按排期自动生成回归测试（用户 2026-09-23 裁决）。
 *
 * <p>阶段计划 = 该阶段排期任务 plannedStart 最小值 / plannedEnd 最大值；项目计划 = 全部阶段最值；
 * 与合同施工计划快照冲突时以排期为准并回写快照；无排期任务的阶段保留原计划窗口。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:plan-from-schedule;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class ProjectPlanFromScheduleTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectStageRepository stageRepository;
    @Autowired private ProjectTaskRepository taskRepository;
    @Autowired private ProjectContractRepository contractRepository;
    @Autowired private ContractConstructionPlanRepository constructionPlanRepository;
    @Autowired private ContractConstructionStageRepository constructionStageRepository;
    @Autowired private ProjectContext projectContext;

    @Test
    void shouldDeriveStageAndProjectPlanFromTaskScheduleAndSyncSnapshot() {
        Fixture f = fixture("plan-derive");
        ProjectStage prepare = f.prepareStage();
        ProjectStage water = f.waterStage();

        // 排期：开工交底 10-01 ~ 10-05 与 10-08 ~ 10-12；水电阶段无任务（保留快照窗口）
        task(f, prepare, "开工交底任务 A", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        task(f, prepare, "开工交底任务 B", LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));

        projectContext.recalculatePlanFromSchedule(f.project());

        ProjectStage afterPrepare = stageRepository.findById(prepare.getStageId()).orElseThrow();
        ProjectStage afterWater = stageRepository.findById(water.getStageId()).orElseThrow();
        assertThat(afterPrepare.getPlannedStart()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(afterPrepare.getPlannedEnd()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(afterPrepare.getAcceptanceMode()).isEqualTo(StageAcceptanceMode.None);
        assertThat(afterWater.getAcceptanceMode()).isEqualTo(StageAcceptanceMode.None);
        assertThat(afterWater.getPlannedStart()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(afterWater.getPlannedEnd()).isEqualTo(LocalDate.of(2026, 10, 24));

        Project project = projectRepository.findById(f.project().getProjectId()).orElseThrow();
        assertThat(project.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(project.getPlanEndDate()).isEqualTo(LocalDate.of(2026, 10, 24));

        ContractConstructionPlan snapshot = constructionPlanRepository
                .findByContract_ContractIdAndCompany_ComId(f.contract().getContractId(), f.company().getComId())
                .orElseThrow();
        assertThat(snapshot.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(snapshot.getEndDate()).isEqualTo(LocalDate.of(2026, 10, 24));
        var snapshotStages = constructionStageRepository.findByPlan_PlanIdOrderBySortAsc(snapshot.getPlanId());
        assertThat(snapshotStages).hasSize(2);
        assertThat(snapshotStages.get(0).getPlannedStart()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(snapshotStages.get(0).getPlannedEnd()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(snapshotStages.get(1).getPlannedStart()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(snapshotStages.get(1).getPlannedEnd()).isEqualTo(LocalDate.of(2026, 10, 24));
    }

    private ProjectTask task(Fixture f, ProjectStage stage, String name, LocalDate start, LocalDate end) {
        ProjectTask task = new ProjectTask();
        task.setCompany(f.company());
        task.setProject(f.project());
        task.setProperty(f.property());
        task.setStage(stage);
        task.setOwner(f.pm());
        task.setCreator(f.pm());
        task.setName(name);
        task.setZone("客厅");
        task.setCraft("免漆板柜体");
        task.setQuantity(BigDecimal.ONE);
        task.setSalePrice(new BigDecimal("1000"));
        task.setCost(new BigDecimal("800"));
        task.setStatus(TaskStatus.Todo);
        task.setProgress(0);
        task.setConceal(false);
        task.setPlannedStart(start);
        task.setPlannedEnd(end);
        return taskRepository.save(task);
    }

    private record Fixture(Company company, Store store, Employee pm, Customer customer, Property property,
                           Quote quote, Project project, ProjectContract contract, ProjectStage prepareStage,
                           ProjectStage waterStage) {
    }

    private Fixture fixture(String prefix) {
        String key = Long.toHexString(System.nanoTime());
        Company company = company("企" + key);
        Store store = store(company);
        Employee pm = employee(company, store, EmployeeRole.Pm);
        Customer customer = customer(company, store, pm, key);
        Property property = property(company, store, customer, pm, key);
        Quote quote = quote(company, store, customer, property, pm, key);
        Project project = project(company, store, customer, property, quote, pm, key);
        ProjectContract contract = contract(company, customer, property, quote, project, pm, key);
        ContractConstructionPlan plan = constructionPlan(company, contract, key);
        ProjectStage prepare = stage(company, project, property, "PREPARE", "开工交底", 1,
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 4));
        ProjectStage water = stage(company, project, property, "WATER_ELECTRIC", "水电施工", 2,
                LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 24));
        constructionStage(company, plan, "PREPARE", "开工交底", 1,
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 4));
        constructionStage(company, plan, "WATER_ELECTRIC", "水电施工", 2,
                LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 24));
        return new Fixture(company, store, pm, customer, property, quote, project, contract, prepare, water);
    }

    private ContractConstructionPlan constructionPlan(Company company, ProjectContract contract, String key) {
        ContractConstructionPlan plan = new ContractConstructionPlan();
        plan.setCompany(company);
        plan.setContract(contract);
        plan.setSourceCode("RESIDENTIAL_STANDARD_8");
        plan.setSourceVersion(1);
        plan.setName("住宅标准八阶段 " + key);
        plan.setStartDate(LocalDate.of(2026, 9, 20));
        plan.setEndDate(LocalDate.of(2026, 10, 24));
        plan.setStatus(PlanSnapshotStatus.Confirmed);
        return constructionPlanRepository.save(plan);
    }

    private ContractConstructionStage constructionStage(Company company, ContractConstructionPlan plan,
                                                        String code, String name, int sort,
                                                        LocalDate start, LocalDate end) {
        ContractConstructionStage stage = new ContractConstructionStage();
        stage.setCompany(company);
        stage.setPlan(plan);
        stage.setCode(code);
        stage.setName(name);
        stage.setSort(sort);
        stage.setDefaultDays(10);
        stage.setPlannedStart(start);
        stage.setPlannedEnd(end);
        stage.setAcceptanceMode(StageAcceptanceMode.None);
        return constructionStageRepository.save(stage);
    }

    private Company company(String title) {
        Company company = new Company();
        company.setTitle(title);
        company.setName("C" + Long.toHexString(System.nanoTime()));
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company) {
        Store store = new Store();
        store.setCompany(company);
        store.setName("S" + Long.toHexString(System.nanoTime()));
        store.setDefaultStore(true);
        return storeRepository.save(store);
    }

    private Employee employee(Company company, Store store, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setName("E" + Long.toHexString(System.nanoTime()));
        employee.setTelNo("1" + Math.abs(System.nanoTime() % 1_000_000_000L));
        return employeeRepository.save(employee);
    }

    private Customer customer(Company company, Store store, Employee owner, String key) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(owner);
        customer.setName("客" + key);
        customer.setTelNo("2" + Math.abs(System.nanoTime() % 1_000_000_000L));
        customer.setCommunity("小" + key);
        customer.setAddr("地" + key);
        customer.setArea(new BigDecimal("100"));
        return customerRepository.save(customer);
    }

    private Property property(Company company, Store store, Customer customer, Employee owner, String key) {
        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(owner);
        property.setCustomer(customer);
        property.setPropertyNo("P-" + key);
        property.setCommunity(customer.getCommunity());
        property.setBuilding("1");
        property.setRoomNo("101");
        property.setAddress(customer.getAddr());
        property.setArea(new BigDecimal("100"));
        property.setLayout("3室2厅");
        property.setStatus(PropertyStatus.Serving);
        return propertyRepository.save(property);
    }

    private Quote quote(Company company, Store store, Customer customer, Property property,
                        Employee owner, String key) {
        Quote quote = new Quote();
        quote.setNo("Q-" + key);
        quote.setTitle("报" + key);
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(owner);
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

    private Project project(Company company, Store store, Customer customer, Property property,
                            Quote quote, Employee pm, String key) {
        Project project = new Project();
        project.setNo("PJ-" + key);
        project.setName("项" + key);
        project.setCompany(company);
        project.setStore(store);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setOwner(customer.getOwner());
        project.setManager(pm);
        project.setSiteAddr(property.getAddress());
        project.setArea(property.getArea());
        project.setContractAmount(new BigDecimal("100000"));
        project.setCost(new BigDecimal("80000"));
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(20);
        project.setStartDate(LocalDate.of(2026, 9, 20));
        project.setPlanEndDate(LocalDate.of(2026, 10, 24));
        return projectRepository.save(project);
    }

    private ProjectContract contract(Company company, Customer customer, Property property, Quote quote,
                                     Project project, Employee creator, String key) {
        ProjectContract contract = new ProjectContract();
        contract.setNo("C" + key);
        contract.setCompany(company);
        contract.setQuote(quote);
        contract.setCustomer(customer);
        contract.setProperty(property);
        contract.setProject(project);
        contract.setBaselineAmount(new BigDecimal("100000"));
        contract.setSignedAmount(new BigDecimal("100000"));
        contract.setCurrentAmount(new BigDecimal("100000"));
        contract.setStatus(ContractStatus.Effective);
        contract.setCreatedBy(creator);
        contract.setSignedAt(LocalDate.of(2026, 9, 19));
        return contractRepository.save(contract);
    }

    private ProjectStage stage(Company company, Project project, Property property, String code, String name,
                               int sort, LocalDate start, LocalDate end) {
        ProjectStage stage = new ProjectStage();
        stage.setCompany(company);
        stage.setProject(project);
        stage.setProperty(property);
        stage.setName(name);
        stage.setStageCode(code);
        stage.setSort(sort);
        stage.setWeight(new BigDecimal("50"));
        stage.setAcceptanceMode(StageAcceptanceMode.None);
        stage.setStatus(StageStatus.Todo);
        stage.setProgress(0);
        stage.setPlannedStart(start);
        stage.setPlannedEnd(end);
        return stageRepository.save(stage);
    }
}
