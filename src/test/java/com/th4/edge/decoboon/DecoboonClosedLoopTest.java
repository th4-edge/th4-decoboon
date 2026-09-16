package com.th4.edge.decoboon;

import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageSectionRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.quote.context.QuoteContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BIM 套餐报价到项目施工验收的闭环集成测试。
 *
 * <p>测试使用真实 saas 同步后的 BIM 数据，事务结束后回滚，不污染目标数据库。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:decoboon-closed-loop;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class DecoboonClosedLoopTest {

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PackageTemplateRepository giftPackRepository;
    @Autowired
    private PackageSectionRepository sectionRepository;
    @Autowired
    private CatalogItemRepository catalogItemRepository;
    @Autowired
    private QuoteContext quoteContext;
    @Autowired
    private QuoteLineRepository quoteItemRepository;
    @Autowired
    private ProjectContext projectContext;
    @Autowired
    private ProjectContractContext projectContractContext;
    @Autowired
    private ProjectStageRepository stageRepository;
    @Autowired
    private ProjectTaskRepository taskRepository;
    @Autowired
    private MaterialPlanRepository materialPlanRepository;

    @Test
    void shouldCreateQuoteProjectAndCloseAcceptance() {
        long seq = System.currentTimeMillis();
        Company company = new Company();
        company.setComId(seq);
        company.setTitle("闭环测试企业");
        company.setName("闭环企业");
        company.setSize(Size.A);
        company = companyRepository.save(company);

        Store store = new Store();
        store.setStoreId(seq + 1);
        store.setCompany(company);
        store.setName("测试门店");
        store.setDefaultStore(true);
        store = storeRepository.save(store);

        Employee employee = new Employee();
        employee.setEmpId(seq + 2);
        employee.setName("测试项目经理");
        employee.setTelNo("19900000001");
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(EmployeeRole.Marketer);
        employee = employeeRepository.save(employee);

        PackageSection section = new PackageSection();
        section.setCompany(company);
        section.setName("全屋");
        section = sectionRepository.save(section);

        CatalogItem construction = catalogItem(company, employee, ItemType.Construction,
                "墙面施工", Unit.Area, "80", "40");
        CatalogItem material = catalogItem(company, employee, ItemType.Material,
                "主材", Unit.Item, "120", "70");

        PackageTemplate pack = new PackageTemplate();
        pack.setCompany(company);
        pack.setName("闭环测试套餐");
        pack.setArea(new BigDecimal("100"));
        pack.setPrice(new BigDecimal("100000"));
        pack.setDiscount(BigDecimal.ONE);
        PackageLine constructionLine = packageLine(company, pack, section, construction,
                "墙面施工", Unit.Area);
        PackageLine materialLine = packageLine(company, pack, section, material,
                "主材", Unit.Item);
        pack.setItems(List.of(constructionLine, materialLine));
        pack = giftPackRepository.save(pack);

        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setName("测试客户");
        customer.setTelNo("19900000002");
        customer.setArea(new BigDecimal("120"));
        customer.setAddr("测试小区 1 栋 101");
        customer.setOwner(employee);
        customer = customerRepository.save(customer);

        Quote quote = quoteContext.create(customer, pack, new BigDecimal("120"), BigDecimal.ONE, employee);
        assertThat(quote.getAmount()).isPositive();
        assertThat(quoteItemRepository.findByQuote(quote)).isNotEmpty();

        quoteContext.submit(quote.getQuoteId());
        quoteContext.accept(quote.getQuoteId());
        ProjectContract contract = projectContractContext.createFromQuote(
                quote, employee, LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(90), "测试合同");
        projectContractContext.markEffective(contract);

        Project draft = new Project();
        draft.setCompany(company);
        draft.setName("闭环测试项目");
        draft.setSiteAddr("测试小区 1 栋 101");
        draft.setStartDate(LocalDate.now());
        draft.setPlanEndDate(LocalDate.now().plusDays(90));
        draft.setManager(employee);
        Project project = projectContext.createFromEffectiveContract(contract, draft);

        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        List<MaterialPlan> materialPlans = materialPlanRepository.findByProject(project);
        assertThat(stages).isNotEmpty();
        assertThat(stages.get(0).getPlannedStart()).isEqualTo(project.getStartDate());
        assertThat(stages.get(stages.size() - 1).getPlannedEnd()).isEqualTo(project.getPlanEndDate());
        assertThat(materialPlans).isNotEmpty();
        Company otherCompany = new Company();
        otherCompany.setTitle("其他闭环企业");
        otherCompany.setName("其他企业");
        otherCompany.setSize(Size.A);
        otherCompany = companyRepository.save(otherCompany);
        Long otherCompanyId = otherCompany.getComId();
        assertThatThrownBy(() -> projectContext.updateMaterialPlan(
                materialPlans.get(0).getPlanId(), otherCompanyId, MaterialPlanStatus.Arrived, LocalDate.now()))
                .hasMessageContaining("无权访问其他企业的项目数据");

        MaterialPlan arrived = projectContext.updateMaterialPlan(
                materialPlans.get(0).getPlanId(), company.getComId(), MaterialPlanStatus.Arrived, LocalDate.now());
        assertThat(arrived.getStatus()).isEqualTo(MaterialPlanStatus.Arrived);
        assertThat(arrived.getArrivedAt()).isNotNull();

        for (ProjectStage stage : stages) {
            for (ProjectTask task : taskRepository.findByStage(stage)) {
                project = projectContext.updateTask(task.getTaskId(), company.getComId(), TaskStatus.Done, List.of("https://example.com/evidence.jpg"))
                        .getProject();
            }
        }
        assertThat(project.getProgress()).isEqualTo(100);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.Accepting);

        ProjectStage firstStage = stages.get(0);
        Acceptance acceptance = projectContext.submitAcceptance(
                company.getComId(), project.getProjectId(), firstStage.getStageId(), employee,
                "施工完成，申请确认", List.of(), List.of("https://example.com/accept.jpg"));
        Acceptance confirmed = projectContext.confirmAcceptance(acceptance.getAcceptanceId(), company.getComId(), true, "客户确认");
        assertThat(confirmed.getStatus().name()).isEqualTo("Confirmed");
        assertThat(stageRepository.findById(firstStage.getStageId()).orElseThrow().getStatus().name()).isEqualTo("Done");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.Finished);
    }

    private CatalogItem catalogItem(Company company, Employee employee, ItemType type,
                                    String name, Unit unit, String materialPrice, String costPrice) {
        CatalogItem item = new CatalogItem();
        item.setCompany(company);
        item.setMaintainer(employee);
        item.setType(type);
        item.setCategory(name);
        item.setName(name);
        item.setUnit(unit);
        item.setPricingMode(PricingMethod.MaterialAndManual);
        item.setMaterialPrice(new BigDecimal(materialPrice));
        item.setManualPrice(BigDecimal.ZERO);
        item.setCostPrice(new BigDecimal(costPrice));
        item.setFormula("固定项");
        item.setActive(true);
        item.setSort(0);
        return catalogItemRepository.save(item);
    }

    private PackageLine packageLine(Company company, PackageTemplate pack, PackageSection section,
                                    CatalogItem item, String name, Unit unit) {
        PackageLine line = new PackageLine();
        line.setCompany(company);
        line.setTemplate(pack);
        line.setSection(section);
        line.setItem(item);
        line.setName(name);
        line.setUnit(unit);
        line.setQuantityFactor(BigDecimal.ONE);
        line.setPricingMethod(PricingMethod.MaterialAndManual);
        line.setIncluded(true);
        line.setDeductible(true);
        line.setSort(0);
        return line;
    }

}
