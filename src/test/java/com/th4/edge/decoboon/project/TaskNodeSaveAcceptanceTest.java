package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.configure.TaskNodeKey;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.model.TaskNode;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.project.repository.TaskNodeRepository;
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
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 施工任务影像节点留证回归测试（DEF-PROJ37-001）。
 *
 * <p>背景：{@link TaskNodeKey} 曾未实现 {@code Enumerable#getValue()}，导致
 * {@code project/task-node/save} 的节点入参解析抛 NPE（响应 {@code undefinedErrorCode}），
 * 施工进行/隐蔽工程/施工完成三类节点在真实链路全部不可用。本测试锁定三节点可用性与留证规则。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:task-node-acceptance;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class TaskNodeSaveAcceptanceTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectStageRepository stageRepository;
    @Autowired private ProjectTaskRepository taskRepository;
    @Autowired private TaskNodeRepository nodeRepository;
    @Autowired private ProjectReportRepository reportRepository;
    @Autowired private CatalogItemRepository catalogItemRepository;
    @Autowired private com.th4.edge.decoboon.project.service.tasknodes.save.Save taskNodeSave;
    @Autowired private com.th4.edge.decoboon.project.service.tasks.save.Save taskSave;

    @BeforeEach
    void unwrapProviderProxy() {
        taskNodeSave = AopTestUtils.getTargetObject(taskNodeSave);
        taskSave = AopTestUtils.getTargetObject(taskSave);
    }

    @Test
    void shouldPersistTaskPropertyWhenSchedulingByProvider() {
        Fixture fixture = fixture("task-schedule", false, EmployeeRole.Pm);
        CatalogItem catalogItem = catalogItem(fixture.company(), fixture.pm());

        ReflectionTestUtils.setField(taskSave, "employee", fixture.pm());
        ReflectionTestUtils.setField(taskSave, "taskId", null);
        ReflectionTestUtils.setField(taskSave, "projectId", fixture.project().getProjectId());
        ReflectionTestUtils.setField(taskSave, "stageId", fixture.stage().getStageId());
        ReflectionTestUtils.setField(taskSave, "catalogId", catalogItem.getItemId());
        ReflectionTestUtils.setField(taskSave, "zoneId", "LivingRoom");
        ReflectionTestUtils.setField(taskSave, "quantity", new BigDecimal("2"));
        ReflectionTestUtils.setField(taskSave, "ownerId", null);
        ReflectionTestUtils.setField(taskSave, "name", "排期新增任务");
        ReflectionTestUtils.setField(taskSave, "craft", "免漆板柜体");
        ReflectionTestUtils.setField(taskSave, "spec", "现场制作");
        ReflectionTestUtils.setField(taskSave, "priority", "normal");
        ReflectionTestUtils.setField(taskSave, "remark", "DEF-PROJ37-005 回归");
        ReflectionTestUtils.setField(taskSave, "plannedStart", fixture.stage().getPlannedStart());
        ReflectionTestUtils.setField(taskSave, "plannedEnd", fixture.stage().getPlannedEnd());

        ProjectTask scheduled = taskSave.call(new ProjectTask());

        assertThat(scheduled.getTaskId()).isNotNull();
        assertThat(scheduled.getProperty().getPropertyId()).isEqualTo(fixture.property().getPropertyId());
        assertThat(scheduled.getProject().getProjectId()).isEqualTo(fixture.project().getProjectId());
        assertThat(scheduled.getStage().getStageId()).isEqualTo(fixture.stage().getStageId());
        assertThat(scheduled.getCatalogItem().getItemId()).isEqualTo(catalogItem.getItemId());
        assertThat(scheduled.getZone()).isEqualTo("客厅");
        assertThat(scheduled.getOwner().getEmpId()).isEqualTo(fixture.pm().getEmpId());
        assertThat(scheduled.getStatus()).isEqualTo(TaskStatus.Todo);

        // 新建任务必须能立即进入影像节点留证（两个缺陷的联合回归）
        confirm(fixture, scheduled, "Start", "排期后开工留证", "upload/project/schedule-start.png");
        ProjectTask afterStart = taskRepository.findById(scheduled.getTaskId()).orElseThrow();
        assertThat(afterStart.getStatus()).isEqualTo(TaskStatus.Doing);
        assertThat(afterStart.getProperty().getPropertyId()).isEqualTo(fixture.property().getPropertyId());
    }

    @Test
    void shouldRejectDuplicateDispatchOfSameCraftInSameStageAndZone() {
        Fixture fixture = fixture("task-duplicate", false, EmployeeRole.Pm);
        CatalogItem catalogItem = catalogItem(fixture.company(), fixture.pm());
        dispatch(fixture, catalogItem, "LivingRoom", new BigDecimal("2"));

        // 同一「项目 × 阶段 × 区域 × 工艺」重复下发必须阻断，提示改为在原任务上调整数量
        ReflectionTestUtils.setField(taskSave, "ownerId", null);
        assertDefined(400, "已在「", "请在原任务上调整数量");
        assertThat(taskRepository.findByProject(fixture.project())).hasSize(2);

        // 换区域仍可下发
        dispatch(fixture, catalogItem, "Kitchen", new BigDecimal("1"));
        assertThat(taskRepository.findByProject(fixture.project())).hasSize(3);
    }

    private void dispatch(Fixture fixture, CatalogItem catalogItem, String zoneId, BigDecimal quantity) {
        ReflectionTestUtils.setField(taskSave, "employee", fixture.pm());
        ReflectionTestUtils.setField(taskSave, "taskId", null);
        ReflectionTestUtils.setField(taskSave, "projectId", fixture.project().getProjectId());
        ReflectionTestUtils.setField(taskSave, "stageId", fixture.stage().getStageId());
        ReflectionTestUtils.setField(taskSave, "catalogId", catalogItem.getItemId());
        ReflectionTestUtils.setField(taskSave, "zoneId", zoneId);
        ReflectionTestUtils.setField(taskSave, "quantity", quantity);
        ReflectionTestUtils.setField(taskSave, "ownerId", null);
        ReflectionTestUtils.setField(taskSave, "name", null);
        ReflectionTestUtils.setField(taskSave, "craft", null);
        ReflectionTestUtils.setField(taskSave, "spec", null);
        ReflectionTestUtils.setField(taskSave, "priority", "normal");
        ReflectionTestUtils.setField(taskSave, "remark", null);
        ReflectionTestUtils.setField(taskSave, "plannedStart", fixture.stage().getPlannedStart());
        ReflectionTestUtils.setField(taskSave, "plannedEnd", fixture.stage().getPlannedEnd());
        taskSave.call(new ProjectTask());
    }

    @Test
    void shouldUpdateExistingTaskInsteadOfCreatingDuplicate() {
        Fixture fixture = fixture("task-schedule-update", false, EmployeeRole.Pm);
        CatalogItem catalogItem = catalogItem(fixture.company(), fixture.pm());

        ReflectionTestUtils.setField(taskSave, "employee", fixture.pm());
        ReflectionTestUtils.setField(taskSave, "taskId", null);
        ReflectionTestUtils.setField(taskSave, "projectId", fixture.project().getProjectId());
        ReflectionTestUtils.setField(taskSave, "stageId", fixture.stage().getStageId());
        ReflectionTestUtils.setField(taskSave, "catalogId", catalogItem.getItemId());
        ReflectionTestUtils.setField(taskSave, "zoneId", "Kitchen");
        ReflectionTestUtils.setField(taskSave, "quantity", BigDecimal.ONE);
        ReflectionTestUtils.setField(taskSave, "ownerId", null);
        ReflectionTestUtils.setField(taskSave, "priority", "normal");
        ReflectionTestUtils.setField(taskSave, "plannedStart", fixture.stage().getPlannedStart());
        ReflectionTestUtils.setField(taskSave, "plannedEnd", fixture.stage().getPlannedEnd());
        ProjectTask created = taskSave.call(new ProjectTask());

        ReflectionTestUtils.setField(taskSave, "taskId", created.getTaskId());
        ReflectionTestUtils.setField(taskSave, "quantity", new BigDecimal("5"));
        ReflectionTestUtils.setField(taskSave, "name", "排期任务（调整总量）");
        ProjectTask updated = taskSave.call(new ProjectTask());

        assertThat(updated.getTaskId()).isEqualTo(created.getTaskId());
        assertThat(updated.getQuantity()).isEqualByComparingTo("5");
        assertThat(taskRepository.findByProject(fixture.project())).hasSize(2);
    }

    @Test
    void taskNodeKeyExposesStableValueForAllThreeNodes() {
        for (TaskNodeKey key : TaskNodeKey.values()) {
            assertThat(key.getValue()).as("节点稳定枚举值不能为空：%s", key.name()).isNotBlank();
            assertThat(key.getValue()).isEqualTo(key.name());
            assertThat(key.getName()).isNotBlank();
        }
        assertThat(TaskNodeKey.values()).containsExactly(TaskNodeKey.Start, TaskNodeKey.Conceal, TaskNodeKey.Finish);
    }

    @Test
    void shouldConfirmStartConcealFinishNodesWithEvidence() {
        Fixture fixture = fixture("node-happy", true, EmployeeRole.Pm);
        ProjectTask task = fixture.task();

        confirm(fixture, task, "Start", "施工进行留证", "upload/project/node-start.png");
        ProjectTask afterStart = taskRepository.findById(task.getTaskId()).orElseThrow();
        assertThat(afterStart.getStatus()).isEqualTo(TaskStatus.Doing);
        assertThat(afterStart.getProgress()).isGreaterThanOrEqualTo(10);

        confirm(fixture, task, "Conceal", "隐蔽工程留证", "upload/project/node-conceal.png");
        ProjectTask afterConceal = taskRepository.findById(task.getTaskId()).orElseThrow();
        assertThat(afterConceal.getProgress()).isGreaterThanOrEqualTo(55);

        confirm(fixture, task, "Finish", "施工完成留证", "upload/project/node-finish.png");
        ProjectTask afterFinish = taskRepository.findById(task.getTaskId()).orElseThrow();
        assertThat(afterFinish.getStatus()).isEqualTo(TaskStatus.Accepting);
        assertThat(afterFinish.getProgress()).isEqualTo(90);
        assertThat(afterFinish.getEvidenceUrls()).hasSize(3);

        List<TaskNode> nodes = nodeRepository.findByTaskOrderByCreateTime_DateTimeAsc(afterFinish);
        assertThat(nodes).extracting(TaskNode::getNodeKey)
                .containsExactly(TaskNodeKey.Start, TaskNodeKey.Conceal, TaskNodeKey.Finish);
        assertThat(nodes).allSatisfy(node -> {
            assertThat(node.getMedias()).isNotEmpty();
            assertThat(node.getConfirmedBy().getEmpId()).isEqualTo(fixture.pm().getEmpId());
            assertThat(node.getCompany().getComId()).isEqualTo(fixture.company().getComId());
            assertThat(node.getOwner().getEmpId()).isEqualTo(fixture.customer().getOwner().getEmpId());
        });

        List<ProjectReport> reports = reportRepository.findAll().stream()
                .filter(report -> report.getTask() != null
                        && report.getTask().getTaskId().equals(task.getTaskId()))
                .toList();
        assertThat(reports).hasSize(3);
        assertThat(reports).allSatisfy(report -> {
            assertThat(report.getType()).isEqualTo(ReportType.Progress);
            assertThat(report.getVisibleToCustomer()).isTrue();
            assertThat(report.getAuthor().getEmpId()).isEqualTo(fixture.pm().getEmpId());
        });

        ProjectStage stage = stageRepository.findById(fixture.stage().getStageId()).orElseThrow();
        assertThat(stage.getStatus()).isEqualTo(StageStatus.Doing);
    }

    @Test
    void shouldRejectNodeWithoutEvidence() {
        Fixture fixture = fixture("node-no-evidence", false, EmployeeRole.Pm);
        setFields(fixture, fixture.task(), "Start", null, List.of());
        assertDefined(400, "施工节点必须上传照片或视频");
        assertThat(nodeRepository.findByTaskOrderByCreateTime_DateTimeAsc(fixture.task())).isEmpty();
    }

    @Test
    void shouldRejectDuplicateNode() {
        Fixture fixture = fixture("node-duplicate", false, EmployeeRole.Pm);
        confirm(fixture, fixture.task(), "Start", "首次留证", "upload/project/dup-1.png");

        setFields(fixture, fixture.task(), "Start", "重复留证", media("upload/project/dup-2.png"));
        assertDefined(400, "该施工节点已经确认");
        assertThat(nodeRepository.findByTaskOrderByCreateTime_DateTimeAsc(fixture.task())).hasSize(1);
    }

    @Test
    void shouldRejectFinishBeforeConcealForConcealTask() {
        Fixture fixture = fixture("node-conceal-order", true, EmployeeRole.Pm);
        confirm(fixture, fixture.task(), "Start", "开工留证", "upload/project/order-start.png");

        setFields(fixture, fixture.task(), "Finish", "隐蔽未确认直接完工", media("upload/project/order-finish.png"));
        assertDefined(400, "隐蔽工程确认后才能完成施工");
    }

    @Test
    void shouldRejectRolesOutsideBossStoreManagerAndProjectManager() {
        Fixture fixture = fixture("node-role", false, EmployeeRole.Designer);
        setFields(fixture, fixture.task(), "Start", "设计身份留证", media("upload/project/role.png"));
        assertDefined(403, "只有企业负责人、店长或项目经理可以确认施工节点");
        assertThat(nodeRepository.findByTaskOrderByCreateTime_DateTimeAsc(fixture.task())).isEmpty();
    }

    @Test
    void shouldAllowStoreManagerOfSameStoreToConfirmNode() {
        Fixture fixture = fixture("node-store-manager", false, EmployeeRole.StoreManager);
        setFields(fixture, fixture.task(), "Start", "店长留证", media("upload/project/store-manager.png"));
        TaskNode node = taskNodeSave.call(new TaskNode());

        assertThat(node.getNodeKey()).isEqualTo(TaskNodeKey.Start);
        assertThat(node.getConfirmedBy().getEmpId()).isEqualTo(fixture.actor().getEmpId());
        assertThat(taskRepository.findById(fixture.task().getTaskId()).orElseThrow().getStatus())
                .isEqualTo(TaskStatus.Doing);
    }

    @Test
    void shouldRejectStoreManagerFromAnotherStore() {
        Fixture fixture = fixture("node-store-isolation", false, EmployeeRole.Pm);
        Store otherStore = store(fixture.company());
        Employee otherStoreManager = employee(fixture.company(), otherStore, EmployeeRole.StoreManager);

        ReflectionTestUtils.setField(taskNodeSave, "employee", otherStoreManager);
        ReflectionTestUtils.setField(taskNodeSave, "taskId", fixture.task().getTaskId());
        ReflectionTestUtils.setField(taskNodeSave, "key", "Start");
        ReflectionTestUtils.setField(taskNodeSave, "note", "跨门店留证");
        ReflectionTestUtils.setField(taskNodeSave, "medias", media("upload/project/cross-store.png"));
        assertDefined(404, "施工任务不存在或无权访问");
        assertThat(nodeRepository.findByTaskOrderByCreateTime_DateTimeAsc(fixture.task())).isEmpty();
    }

    @Test
    void shouldParseKeyCaseInsensitivelyAndRejectUnknownOrBlank() {
        Fixture fixture = fixture("node-parse", false, EmployeeRole.Pm);

        setFields(fixture, fixture.task(), "start", "小写节点名", media("upload/project/parse-1.png"));
        TaskNode node = taskNodeSave.call(new TaskNode());
        assertThat(node.getNodeKey()).isEqualTo(TaskNodeKey.Start);

        Fixture unknown = fixture("node-parse-unknown", false, EmployeeRole.Pm);
        setFields(unknown, unknown.task(), "NotANode", "非法节点", media("upload/project/parse-2.png"));
        assertDefined(400, "不支持的施工节点");

        Fixture blank = fixture("node-parse-blank", false, EmployeeRole.Pm);
        setFields(blank, blank.task(), "   ", "空节点", media("upload/project/parse-3.png"));
        assertDefined(400, "节点类型不能为空");
    }

    private void confirm(Fixture fixture, ProjectTask task, String key, String note, String url) {
        setFields(fixture, task, key, note, media(url));
        taskNodeSave.call(new TaskNode());
    }

    private List<Map<String, Object>> media(String url) {
        return List.of(Map.of("url", url));
    }

    private void setFields(Fixture fixture, ProjectTask task, String key, String note, List<Map<String, Object>> medias) {
        setField("employee", fixture.actor());
        setField("taskId", task.getTaskId());
        setField("key", key);
        setField("note", note);
        setField("medias", medias);
    }

    private void setField(String name, Object value) {
        ReflectionTestUtils.setField(taskNodeSave, name, value);
    }

    private void assertDefined(int code, String messagePart) {
        assertThatThrownBy(() -> taskNodeSave.call(new TaskNode()))
                .isInstanceOfSatisfying(DefinedException.class, error -> {
                    assertThat(error.getErrorCode().getCode()).isEqualTo(String.valueOf(code));
                    assertThat(error.getMessage()).contains(messagePart);
                });
    }

    private void assertDefined(int code, String messagePart, String secondMessagePart) {
        assertThatThrownBy(() -> taskSave.call(new ProjectTask()))
                .isInstanceOfSatisfying(DefinedException.class, error -> {
                    assertThat(error.getErrorCode().getCode()).isEqualTo(String.valueOf(code));
                    assertThat(error.getMessage()).contains(messagePart).contains(secondMessagePart);
                });
    }

    private record Fixture(Company company, Store store, Employee pm, Employee actor,
                           Customer customer, Property property, Project project,
                           ProjectStage stage, ProjectTask task) {
    }

    private Fixture fixture(String prefix, boolean conceal, EmployeeRole actorRole) {
        String key = Long.toHexString(System.nanoTime());
        Company company = company("企" + key);
        Store store = store(company);
        Employee pm = employee(company, store, EmployeeRole.Pm);
        Employee actor = actorRole == EmployeeRole.Pm ? pm : employee(company, store, actorRole);
        Customer customer = customer(company, store, pm, key);
        Property property = property(company, store, customer, pm, key);
        Quote quote = quote(company, store, customer, property, pm, key);
        Project project = project(company, store, customer, property, quote, pm, key);
        if (actor.getEmpId() != null && !actor.getEmpId().equals(pm.getEmpId())) {
            // 非项目经理角色必须能看见该项目，才能命中「角色无权限」分支（403）而不是 404。
            project.setDesigner(actor);
            project = projectRepository.save(project);
        }
        ProjectStage stage = stage(company, project, property, key);
        ProjectTask task = task(company, project, property, stage, pm, key, conceal);
        return new Fixture(company, store, pm, actor, customer, property, project, stage, task);
    }

    private CatalogItem catalogItem(Company company, Employee maintainer) {
        CatalogItem item = new CatalogItem();
        item.setCompany(company);
        item.setMaintainer(maintainer);
        item.setCode("C" + Long.toHexString(System.nanoTime()));
        item.setName("工艺条目" + Long.toHexString(System.nanoTime()));
        item.setType(ItemType.Construction);
        item.setCategory("木作");
        item.setFormula("数量×单价");
        item.setCraft("免漆板柜体");
        item.setSpec("现场制作");
        item.setUnit(com.th4.edge.decoboon.bim.configure.Unit.Item);
        item.setMaterialPrice(new BigDecimal("100"));
        item.setManualPrice(new BigDecimal("50"));
        item.setCostPrice(new BigDecimal("120"));
        return catalogItemRepository.save(item);
    }

    private Company company(String name) {
        Company company = new Company();
        company.setTitle(name);
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
        project.setProgress(0);
        project.setStartDate(LocalDate.now());
        project.setPlanEndDate(LocalDate.now().plusDays(30));
        return projectRepository.save(project);
    }

    private ProjectStage stage(Company company, Project project, Property property, String key) {
        ProjectStage stage = new ProjectStage();
        stage.setCompany(company);
        stage.setProject(project);
        stage.setProperty(property);
        stage.setName("木作吊顶" + key);
        stage.setStageCode("CARPENTRY");
        stage.setSort(1);
        stage.setWeight(new BigDecimal("100"));
        stage.setStatus(StageStatus.Todo);
        stage.setProgress(0);
        stage.setPlannedStart(project.getStartDate());
        stage.setPlannedEnd(project.getPlanEndDate());
        return stageRepository.save(stage);
    }

    private ProjectTask task(Company company, Project project, Property property, ProjectStage stage,
                             Employee owner, String key, boolean conceal) {
        ProjectTask task = new ProjectTask();
        task.setCompany(company);
        task.setProject(project);
        task.setProperty(property);
        task.setStage(stage);
        task.setOwner(owner);
        task.setCreator(owner);
        task.setName("木工制作" + key);
        task.setZone("客餐厅");
        task.setCraft("免漆板柜体");
        task.setSpec("现场制作");
        task.setQuantity(new BigDecimal("10"));
        task.setSalePrice(new BigDecimal("1000"));
        task.setCost(new BigDecimal("800"));
        task.setStatus(TaskStatus.Todo);
        task.setProgress(0);
        task.setConceal(conceal);
        task.setPlannedStart(stage.getPlannedStart());
        task.setPlannedEnd(stage.getPlannedEnd());
        return taskRepository.save(task);
    }
}
