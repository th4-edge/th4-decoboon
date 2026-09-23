package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 合同变更到施工任务、材料计划和项目成本的履约映射测试。 */
class ContractChangeFulfillmentContextTest {

    private ContractChangeItemRepository itemRepository;
    private ProjectStageRepository stageRepository;
    private ProjectTaskRepository taskRepository;
    private MaterialPlanRepository materialPlanRepository;
    private ProjectReportRepository reportRepository;
    private ContractChangeFulfillmentContext context;

    @BeforeEach
    void setUp() {
        itemRepository = mock(ContractChangeItemRepository.class);
        stageRepository = mock(ProjectStageRepository.class);
        taskRepository = mock(ProjectTaskRepository.class);
        materialPlanRepository = mock(MaterialPlanRepository.class);
        reportRepository = mock(ProjectReportRepository.class);
        when(stageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(materialPlanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var returnRepository = mock(com.th4.edge.decoboon.project.repository.MaterialReturnRepository.class);
        var purchaseRepository = mock(com.th4.edge.decoboon.project.repository.PurchaseOrderRepository.class);
        var payableRepository = mock(com.th4.edge.decoboon.finance.repository.PayableRepository.class);
        when(returnRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payableRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(any())).thenReturn(List.of());
        var purchaseItemRepository = mock(com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository.class);
        var partnerCreditRepository = mock(com.th4.edge.decoboon.finance.repository.PartnerCreditRepository.class);
        when(partnerCreditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var planStatusContext = new MaterialPlanStatusContext(purchaseRepository, purchaseItemRepository);
        var deductionContext = new MaterialDeductionContext(returnRepository, purchaseRepository,
                purchaseItemRepository, materialPlanRepository, payableRepository, partnerCreditRepository,
                planStatusContext, new ProjectPermissionContext(),
                mock(com.th4.edge.components.data.context.ModelIdentityContext.class),
                mock(com.th4.edge.decoboon.supply.context.InventoryContext.class),
                mock(com.th4.edge.decoboon.supply.repository.InventoryStockRepository.class));
        context = new ContractChangeFulfillmentContext(itemRepository, stageRepository,
                taskRepository, materialPlanRepository, reportRepository,
                new ProjectPermissionContext(), new MaterialPlanContext(), deductionContext,
                mock(com.th4.edge.decoboon.material.context.MaterialCategoryContext.class));
    }

    @Test
    void shouldExpandAddChangeIntoTasksAndMaterialPlans() {
        Fixture fixture = fixture();
        ContractChangeItem construction = item(1L, ContractChangeType.Add, ItemType.Construction,
                null, new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("120"));
        ContractChangeItem material = item(2L, ContractChangeType.Add, ItemType.Material,
                null, new BigDecimal("3"), new BigDecimal("50"), new BigDecimal("150"), new BigDecimal("90"));
        when(itemRepository.findByChange_ChangeIdOrderByChangeItemIdAsc(10L))
                .thenReturn(List.of(construction, material));
        when(stageRepository.findByProjectOrderBySort(fixture.project())).thenReturn(List.of());
        when(taskRepository.findByProject(fixture.project())).thenReturn(List.of());
        when(materialPlanRepository.findByProject(fixture.project())).thenReturn(List.of());

        context.apply(fixture.change(), fixture.operator());

        ArgumentCaptor<ProjectTask> task = ArgumentCaptor.forClass(ProjectTask.class);
        ArgumentCaptor<MaterialPlan> plan = ArgumentCaptor.forClass(MaterialPlan.class);
        verify(taskRepository).save(task.capture());
        verify(materialPlanRepository).save(plan.capture());
        assertThat(task.getValue().getChangeItem()).isSameAs(construction);
        assertThat(task.getValue().getSalePrice()).isEqualByComparingTo("200");
        assertThat(task.getValue().getCost()).isEqualByComparingTo("120");
        assertThat(task.getValue().getStore()).isSameAs(fixture.store());
        assertThat(task.getValue().getTeam()).isSameAs(fixture.team());
        assertThat(plan.getValue().getChangeItem()).isSameAs(material);
        assertThat(plan.getValue().getCost()).isEqualByComparingTo("90");
        assertThat(plan.getValue().getOwner()).isSameAs(fixture.owner());
    }

    @Test
    void shouldCancelUnstartedTaskAndCloseUnpurchasedMaterialOnRemove() {
        Fixture fixture = fixture();
        ContractLine line = line(11L, ItemType.Construction, "墙面");
        ProjectTask task = task(fixture.project(), line, TaskStatus.Todo);
        MaterialPlan plan = material(fixture.project(), line, MaterialPlanStatus.Planned);
        when(itemRepository.findByChange_ChangeIdOrderByChangeItemIdAsc(10L))
                .thenReturn(List.of(
                        item(1L, ContractChangeType.Remove, ItemType.Construction, line,
                                new BigDecimal("0"), null, new BigDecimal("-100"), new BigDecimal("-60")),
                        item(2L, ContractChangeType.Remove, ItemType.Material, line,
                                new BigDecimal("0"), null, new BigDecimal("-80"), new BigDecimal("-50"))));
        when(stageRepository.findByProjectOrderBySort(fixture.project())).thenReturn(List.of());
        when(taskRepository.findByProject(fixture.project())).thenReturn(List.of(task));
        when(materialPlanRepository.findByProject(fixture.project())).thenReturn(List.of(plan));

        context.apply(fixture.change(), fixture.operator());

        assertThat(task.getStatus()).isEqualTo(TaskStatus.Cancelled);
        // 减项走扣减流程：数量冲减为 0 后计划行取消（DEC-MP-02）
        assertThat(plan.getQuantity()).isEqualByComparingTo("0");
        assertThat(plan.getStatus()).isEqualTo(MaterialPlanStatus.Cancelled);
    }

    @Test
    void shouldReplaceUnstartedScopeAndKeepOneTraceableReplacement() {
        Fixture fixture = fixture();
        ContractLine line = line(11L, ItemType.Construction, "墙面");
        ProjectTask task = task(fixture.project(), line, TaskStatus.Todo);
        MaterialPlan plan = material(fixture.project(), line, MaterialPlanStatus.Planned);
        ContractChangeItem construction = item(1L, ContractChangeType.Replace, ItemType.Construction, line,
                new BigDecimal("4"), new BigDecimal("120"), new BigDecimal("180"), new BigDecimal("100"));
        ContractChangeItem material = item(2L, ContractChangeType.Replace, ItemType.Material, line,
                new BigDecimal("4"), new BigDecimal("70"), new BigDecimal("120"), new BigDecimal("80"));
        when(itemRepository.findByChange_ChangeIdOrderByChangeItemIdAsc(10L))
                .thenReturn(List.of(construction, material));
        when(stageRepository.findByProjectOrderBySort(fixture.project())).thenReturn(List.of());
        when(taskRepository.findByProject(fixture.project())).thenReturn(List.of(task));
        when(materialPlanRepository.findByProject(fixture.project())).thenReturn(List.of(plan));

        context.apply(fixture.change(), fixture.operator());

        assertThat(task.getStatus()).isEqualTo(TaskStatus.Cancelled);
        assertThat(plan.getStatus()).isEqualTo(MaterialPlanStatus.Closed);
        ArgumentCaptor<ProjectTask> tasks = ArgumentCaptor.forClass(ProjectTask.class);
        ArgumentCaptor<MaterialPlan> plans = ArgumentCaptor.forClass(MaterialPlan.class);
        verify(taskRepository, times(2)).save(tasks.capture());
        verify(materialPlanRepository, times(2)).save(plans.capture());
        assertThat(tasks.getAllValues().get(1).getChangeItem()).isSameAs(construction);
        assertThat(tasks.getAllValues().get(1).getSalePrice()).isEqualByComparingTo("480");
        assertThat(plans.getAllValues().get(1).getChangeItem()).isSameAs(material);
        assertThat(plans.getAllValues().get(1).getQuantity()).isEqualByComparingTo("4");
    }

    @Test
    void shouldAdjustUnstartedQuantityAndPriceWithoutCreatingDuplicateObjects() {
        Fixture fixture = fixture();
        ContractLine line = line(11L, ItemType.Construction, "墙面");
        ProjectTask task = task(fixture.project(), line, TaskStatus.Todo);
        MaterialPlan plan = material(fixture.project(), line, MaterialPlanStatus.Planned);
        task.setSalePrice(new BigDecimal("100"));
        task.setCost(new BigDecimal("60"));
        plan.setCost(new BigDecimal("50"));
        when(itemRepository.findByChange_ChangeIdOrderByChangeItemIdAsc(10L))
                .thenReturn(List.of(
                        item(1L, ContractChangeType.Quantity, ItemType.Construction, line,
                                new BigDecimal("3"), null, new BigDecimal("80"), new BigDecimal("40")),
                        item(2L, ContractChangeType.Price, ItemType.Material, line,
                                null, new BigDecimal("70"), new BigDecimal("60"), new BigDecimal("30"))));
        when(stageRepository.findByProjectOrderBySort(fixture.project())).thenReturn(List.of());
        when(taskRepository.findByProject(fixture.project())).thenReturn(List.of(task));
        when(materialPlanRepository.findByProject(fixture.project())).thenReturn(List.of(plan));

        context.apply(fixture.change(), fixture.operator());

        assertThat(task.getQuantity()).isEqualByComparingTo("3");
        assertThat(task.getSalePrice()).isEqualByComparingTo("180");
        assertThat(task.getCost()).isEqualByComparingTo("100");
        assertThat(plan.getUnitPrice()).isEqualByComparingTo("70");
        assertThat(plan.getCost()).isEqualByComparingTo("80");
    }

    private Fixture fixture() {
        Company company = new Company();
        company.setComId(1L);
        Employee owner = new Employee();
        owner.setEmpId(2L);
        owner.setCompany(company);
        com.th4.edge.tenant.user.model.Store store = new com.th4.edge.tenant.user.model.Store();
        store.setStoreId(3L);
        store.setCompany(company);
        com.th4.edge.tenant.user.model.Team team = new com.th4.edge.tenant.user.model.Team();
        team.setTeamId(4L);
        team.setCompany(company);
        Property property = new Property();
        property.setPropertyId(5L);
        property.setCompany(company);
        property.setOwner(owner);
        property.setStore(store);
        property.setTeam(team);
        Project project = new Project();
        project.setProjectId(6L);
        project.setCompany(company);
        project.setProperty(property);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(7L);
        contract.setCompany(company);
        contract.setProject(project);
        contract.setProperty(property);
        ContractChange change = new ContractChange();
        change.setChangeId(10L);
        change.setCompany(company);
        change.setProject(project);
        change.setProperty(property);
        change.setContract(contract);
        return new Fixture(company, owner, store, team, property, project, change);
    }

    private ContractChangeItem item(Long id, ContractChangeType type, ItemType itemType,
                                    ContractLine source, BigDecimal quantity, BigDecimal price,
                                    BigDecimal amount, BigDecimal cost) {
        ContractChangeItem item = new ContractChangeItem();
        item.setChangeItemId(id);
        item.setChangeType(type);
        item.setItemType(itemType);
        item.setSourceLine(source);
        item.setName(itemType.getName() + "变更");
        item.setOldQuantity(source == null ? BigDecimal.ZERO : source.getQuantity());
        item.setNewQuantity(quantity);
        item.setOldPrice(source == null ? BigDecimal.ZERO : new BigDecimal("50"));
        item.setNewPrice(price);
        item.setAmountDelta(amount);
        item.setCostDelta(cost);
        return item;
    }

    private ContractLine line(Long id, ItemType type, String category) {
        ContractLine line = new ContractLine();
        line.setLineId(id);
        line.setType(type);
        line.setCategory(category);
        line.setName(category);
        line.setUnit(Unit.Area);
        line.setQuantity(new BigDecimal("2"));
        line.setMaterialPrice(new BigDecimal("50"));
        line.setManualPrice(BigDecimal.ZERO);
        line.setAmount(new BigDecimal("100"));
        line.setCost(new BigDecimal("60"));
        return line;
    }

    private ProjectTask task(Project project, ContractLine line, TaskStatus status) {
        ProjectTask task = new ProjectTask();
        task.setTaskId(20L);
        task.setProject(project);
        task.setProperty(project.getProperty());
        task.setContractLine(line);
        task.setStatus(status);
        task.setProgress(0);
        task.setQuantity(line.getQuantity());
        return task;
    }

    private MaterialPlan material(Project project, ContractLine line, MaterialPlanStatus status) {
        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(30L);
        plan.setProject(project);
        plan.setProperty(project.getProperty());
        plan.setContractLine(line);
        plan.setStatus(status);
        plan.setQuantity(line.getQuantity());
        plan.setUnitPrice(new BigDecimal("50"));
        plan.setCost(new BigDecimal("50"));
        return plan;
    }

    private record Fixture(Company company, Employee owner, com.th4.edge.tenant.user.model.Store store,
                           com.th4.edge.tenant.user.model.Team team, Property property,
                           Project project, ContractChange change) {
        private Employee operator() {
            return owner;
        }
    }
}
