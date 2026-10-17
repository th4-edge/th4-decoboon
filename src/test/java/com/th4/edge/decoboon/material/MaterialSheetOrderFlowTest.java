package com.th4.edge.decoboon.material;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.common.access.StoreAttributionTestSupport;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.context.PartnerCreditContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.material.configure.MaterialSheetStatus;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.context.MaterialSheetContext;
import com.th4.edge.decoboon.material.context.MaterialSheetPriceProjectionContext;
import com.th4.edge.decoboon.material.context.MaterialSheetTemplateContext;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialOrderSheetItemRepository;
import com.th4.edge.decoboon.material.repository.MaterialOrderSheetRepository;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.MaterialSourceType;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.MaterialPlanStatusContext;
import com.th4.edge.decoboon.project.context.MaterialPurchaseContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.MaterialSupplierRepository;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 主材下单作业单端到端定向流程（SOL-014 / OBJ-008 / DEC-072 / DEC-073）。
 *
 * <p>覆盖：建单（成本快照与损耗继承）→ 三道门禁（未选材、超量）→ 提交请购 →
 * 无价下单（priceState=Unpriced + 暂估应付）与有价下单 → 已下单后禁止删除。</p>
 */
class MaterialSheetOrderFlowTest {

    private final MaterialOrderSheetRepository sheetRepository = mock(MaterialOrderSheetRepository.class);
    private final MaterialOrderSheetItemRepository itemRepository = mock(MaterialOrderSheetItemRepository.class);
    private final MaterialPlanRepository planRepository = mock(MaterialPlanRepository.class);
    private final ProjectContractRepository contractRepository = mock(ProjectContractRepository.class);
    private final PurchaseOrderRepository purchaseRepository = mock(PurchaseOrderRepository.class);
    private final PurchaseOrderItemRepository purchaseItemRepository = mock(PurchaseOrderItemRepository.class);
    private final PartnerRepository partnerRepository = mock(PartnerRepository.class);
    private final PayableRepository payableRepository = mock(PayableRepository.class);
    private final ProjectAccessContext accessContext = mock(ProjectAccessContext.class);
    private final ProjectPermissionContext permissionContext = mock(ProjectPermissionContext.class);
    private final ModelIdentityContext identityContext = mock(ModelIdentityContext.class);
    private final TenantContext tenantContext = mock(TenantContext.class);
    private final MaterialSheetTemplateContext templateContext = mock(MaterialSheetTemplateContext.class);
    private final MaterialPlanStatusContext planStatusContext = mock(MaterialPlanStatusContext.class);
    private final MaterialSelectionRepository selectionRepository = mock(MaterialSelectionRepository.class);

    private final MaterialPurchaseContext purchaseContext = StoreAttributionTestSupport.inject(
            new MaterialPurchaseContext(purchaseRepository, purchaseItemRepository,
                    planRepository, planStatusContext, payableRepository, identityContext,
                    mock(PartnerCreditContext.class), mock(MaterialSupplierRepository.class),
                    mock(PartnerPriceRepository.class)));
    private final MaterialSelectionContext selectionContext =
            new MaterialSelectionContext(selectionRepository, null, null, null, null, null, null);
    private final MaterialSheetPriceProjectionContext priceProjectionContext =
            new MaterialSheetPriceProjectionContext(itemRepository, purchaseItemRepository, sheetRepository);

    private final MaterialSheetContext context = new MaterialSheetContext(
            sheetRepository, itemRepository, planRepository, contractRepository, purchaseRepository,
            purchaseItemRepository, partnerRepository, accessContext, permissionContext, identityContext, tenantContext,
            purchaseContext, selectionContext, templateContext, priceProjectionContext);

    private Company company;
    private Project project;
    private ProjectContract contract;
    private Employee designer;
    private Employee boss;
    private Partner supplier;
    private final List<MaterialOrderSheetItem> savedItems = new ArrayList<>();
    private final List<PurchaseOrderItem> savedPurchaseItems = new ArrayList<>();
    private final List<PurchaseOrder> savedPurchases = new ArrayList<>();

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setComId(1L);
        company.setName("麦鸿装饰");

        Property property = new Property();
        property.setPropertyId(7L);
        property.setCompany(company);

        project = new Project();
        project.setProjectId(9L);
        project.setCompany(company);
        project.setProperty(property);

        contract = new ProjectContract();
        contract.setContractId(11L);
        contract.setProject(project);

        designer = employee(EmployeeRole.Designer);
        boss = employee(EmployeeRole.Boss);

        supplier = new Partner();
        supplier.setPartnerId(3L);
        supplier.setCompany(company);
        supplier.setName("利家福");
        supplier.setType(PartnerType.Supplier);
        supplier.setStatus(PartnerStatus.Active);

        // 下单编排会以财务/BOSS 身份进入，requireEmployee 必须返回调用方传入的身份
        when(accessContext.requireEmployee(any())).thenAnswer(invocation ->
                invocation.getArgument(0) == null ? designer : invocation.getArgument(0));
        when(accessContext.requireProject(any(), any())).thenReturn(project);
        when(accessContext.visibleChild(any(), any(), any())).thenReturn((root, query, builder) -> builder.conjunction());
        when(tenantContext.requireEmployee(any())).thenAnswer(invocation ->
                invocation.getArgument(0) == null ? designer : invocation.getArgument(0));
        when(contractRepository.findByProject(project)).thenReturn(Optional.of(contract));
        when(partnerRepository.findById(3L)).thenReturn(Optional.of(supplier));
        when(identityContext.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sheetRepository.save(any())).thenAnswer(invocation -> {
            MaterialOrderSheet sheet = invocation.getArgument(0);
            if (sheet.getSheetId() == null) sheet.setSheetId(1000L);
            return sheet;
        });
        when(sheetRepository.findOne(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<MaterialOrderSheet>>any())).thenAnswer(invocation -> Optional.of(currentSheet));
        when(itemRepository.save(any())).thenAnswer(invocation -> {
            MaterialOrderSheetItem item = invocation.getArgument(0);
            if (item.getItemId() == null) item.setItemId((long) (500 + savedItems.size()));
            savedItems.add(item);
            return item;
        });
        when(itemRepository.findBySheet_SheetIdOrderBySortAscItemIdAsc(any()))
                .thenAnswer(invocation -> new ArrayList<>(savedItems));
        when(purchaseRepository.save(any())).thenAnswer(invocation -> {
            PurchaseOrder order = invocation.getArgument(0);
            if (order.getPurchaseId() == null) order.setPurchaseId((long) (2000 + savedPurchases.size()));
            if (!savedPurchases.contains(order)) savedPurchases.add(order);
            return order;
        });
        when(purchaseRepository.findByNo(any())).thenAnswer(invocation -> savedPurchases.stream()
                .filter(order -> order.getNo() != null && order.getNo().equals(invocation.getArgument(0)))
                .findFirst());
        when(purchaseItemRepository.save(any())).thenAnswer(invocation -> {
            PurchaseOrderItem item = invocation.getArgument(0);
            if (item.getPurchaseItemId() == null) item.setPurchaseItemId((long) (600 + savedPurchaseItems.size()));
            if (!savedPurchaseItems.contains(item)) savedPurchaseItems.add(item);
            return item;
        });
        when(purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(any()))
                .thenAnswer(invocation -> savedPurchaseItems.stream()
                        .filter(item -> item.getPurchaseOrder() == invocation.getArgument(0))
                        .toList());
        when(purchaseItemRepository.findByMaterialPlan_PlanIdIn(any())).thenReturn(savedPurchaseItems);
        when(payableRepository.findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(any(), any()))
                .thenReturn(Optional.empty());
        when(payableRepository.findFirstByCompany_ComIdAndSourceNoAndType(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(payableRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(planStatusContext.orderedQuantity(any())).thenReturn(BigDecimal.ZERO);
        when(planStatusContext.resolve(any())).thenReturn(MaterialPlanStatus.PartiallyOrdered);
        when(templateContext.requiredHeaderFields(any(), any())).thenReturn(List.of(
                new MaterialSheetTemplateContext.FieldRequirement("measureAt", "测量时间", true, true, 2)));
    }

    private MaterialOrderSheet currentSheet;

    @Test
    void saveShouldCopyCostSnapshotAndLossFromPlan() {
        MaterialPlan plan = plan(1L, new BigDecimal("10"), new BigDecimal("0.08"), true);
        stubPlan(plan);

        MaterialOrderSheet sheet = context.create(designer, 9L, "Tile", "诗尔曼", null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10"), "客厅地砖")));

        assertThat(sheet.getStatus()).isEqualTo(MaterialSheetStatus.Draft);
        assertThat(sheet.getSheetNo()).startsWith("TILE-");
        assertThat(savedItems).hasSize(1);
        MaterialOrderSheetItem saved = savedItems.get(0);
        assertThat(saved.getQuoteCostPrice()).isEqualByComparingTo("100");
        assertThat(saved.getLossRate()).isEqualByComparingTo("0.08");
        assertThat(saved.getPriceState()).isEqualTo(SheetPriceState.Unpriced);
        assertThat(saved.getOrderedQuantity()).isEqualByComparingTo("0");
    }

    @Test
    void lineWithoutConfirmedSelectionMustBeRejected() {
        MaterialPlan plan = plan(2L, new BigDecimal("10"), BigDecimal.ZERO, false);
        stubPlan(plan);

        assertThatThrownBy(() -> context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("5"), "厨房地砖"))))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("尚未完成主材选材");
    }

    @Test
    void quantityAboveNetPlusLossMustBeRejected() {
        MaterialPlan plan = plan(3L, new BigDecimal("10"), BigDecimal.ZERO, true);
        stubPlan(plan);

        assertThatThrownBy(() -> context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10.5"), "阳台地砖"))))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("超过可下单数量");
    }

    @Test
    void submitWithoutRequiredMeasurementMustBeRejected() {
        MaterialPlan plan = plan(4L, new BigDecimal("10"), BigDecimal.ZERO, true);
        stubPlan(plan);
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10"), "客厅地砖")));

        assertThatThrownBy(() -> context.submit(designer, currentSheet.getSheetId()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("测量时间");
    }

    @Test
    void submitAfterMeasurementMovesToSubmitted() {
        MaterialPlan plan = plan(4L, new BigDecimal("10"), BigDecimal.ZERO, true);
        stubPlan(plan);
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10"), "客厅地砖")));
        currentSheet = context.measure(designer, currentSheet.getSheetId(), LocalDate.now(),
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(8), null);

        MaterialOrderSheet submitted = context.submit(designer, currentSheet.getSheetId());

        assertThat(submitted.getStatus()).isEqualTo(MaterialSheetStatus.Submitted);
    }

    @Test
    void cancelBeforeOrderShouldMoveToCancelled() {
        MaterialPlan plan = plan(8L, new BigDecimal("10"), BigDecimal.ZERO, true);
        stubPlan(plan);
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10"), "客厅地砖")));

        MaterialOrderSheet cancelled = context.cancel(designer, currentSheet.getSheetId(), "客户取消");

        assertThat(cancelled.getStatus()).isEqualTo(MaterialSheetStatus.Cancelled);
        assertThat(cancelled.getRemark()).contains("取消原因：客户取消");
    }

    @Test
    void cancelAfterOrderedMustBeRejected() {
        MaterialPlan plan = plan(9L, new BigDecimal("10"), BigDecimal.ZERO, true);
        stubPlan(plan);
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10"), "客厅地砖")));
        currentSheet.setStatus(MaterialSheetStatus.Ordered);

        assertThatThrownBy(() -> context.cancel(designer, currentSheet.getSheetId(), "客户取消"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能直接取消");
    }

    @Test
    void unpricedOrderShouldCreateUnpricedPurchaseAndEstimatedPayable() {
        MaterialPlan plan = plan(5L, new BigDecimal("10"), BigDecimal.ZERO, true);
        stubPlan(plan);
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10"), "客厅地砖")));
        currentSheet.setStatus(MaterialSheetStatus.Submitted);

        ArgumentCaptor<PurchaseOrder> purchases = ArgumentCaptor.forClass(PurchaseOrder.class);
        ArgumentCaptor<Payable> payables = ArgumentCaptor.forClass(Payable.class);

        MaterialOrderSheetItem command = new MaterialOrderSheetItem();
        command.setItemId(savedItems.get(0).getItemId());
        command.setQuantity(new BigDecimal("10"));
        command.setPurchasePrice(null);

        MaterialOrderSheet ordered = context.order(boss, currentSheet.getSheetId(), 3L, null, null, null, null,
                List.of(command));

        assertThat(ordered.getStatus()).isEqualTo(MaterialSheetStatus.Ordered);
        assertThat(ordered.getPriceState()).isEqualTo(SheetPriceState.Unpriced);
        assertThat(savedItems.get(0).getOrderedQuantity()).isEqualByComparingTo("10");

        org.mockito.Mockito.verify(purchaseRepository, org.mockito.Mockito.atLeastOnce()).save(purchases.capture());
        PurchaseOrder purchase = purchases.getAllValues().get(0);
        assertThat(purchase.getPriceState()).isEqualTo(SheetPriceState.Unpriced);
        assertThat(purchase.getAmount()).isEqualByComparingTo("0");
        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.Ordered);

        org.mockito.Mockito.verify(payableRepository).save(payables.capture());
        Payable payable = payables.getValue();
        assertThat(payable.getEstimated())
                .as("无价下单必须形成暂估应付（DEC-072 第 1 项）")
                .isTrue();
        assertThat(payable.getAmount()).isEqualByComparingTo("0");
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.Pending);
        assertThat(payable.getRemark()).contains("暂估");
    }

    @Test
    void pricedOrderShouldCreatePayableWithAmount() {
        MaterialPlan plan = plan(6L, new BigDecimal("10"), BigDecimal.ZERO, true);
        stubPlan(plan);
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10"), "客厅地砖")));
        currentSheet.setStatus(MaterialSheetStatus.Submitted);

        MaterialOrderSheetItem command = new MaterialOrderSheetItem();
        command.setItemId(savedItems.get(0).getItemId());
        command.setQuantity(new BigDecimal("10"));
        command.setPurchasePrice(new BigDecimal("68"));

        ArgumentCaptor<Payable> payables = ArgumentCaptor.forClass(Payable.class);
        MaterialOrderSheet ordered = context.order(boss, currentSheet.getSheetId(), 3L, null, null, null, null,
                List.of(command));

        assertThat(ordered.getPriceState()).isEqualTo(SheetPriceState.Priced);
        assertThat(savedItems.get(0).getPurchasePrice()).isEqualByComparingTo("68");
        org.mockito.Mockito.verify(payableRepository).save(payables.capture());
        assertThat(payables.getValue().getEstimated()).isFalse();
        assertThat(payables.getValue().getAmount()).isEqualByComparingTo("680");
    }

    @Test
    void multiLineOrderShouldCreateOnePurchaseWithItemsAndMixedEstimate() {
        MaterialPlan firstPlan = plan(51L, new BigDecimal("10"), BigDecimal.ZERO, true);
        MaterialPlan secondPlan = plan(52L, new BigDecimal("20"), BigDecimal.ZERO, true);
        when(planRepository.findByProject_ProjectIdAndPlanIdIn(any(), any()))
                .thenReturn(List.of(firstPlan, secondPlan));
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(firstPlan, new BigDecimal("10"), "客厅地砖"),
                        line(secondPlan, new BigDecimal("20"), "厨房墙砖")));
        currentSheet.setStatus(MaterialSheetStatus.Submitted);

        MaterialOrderSheetItem priced = new MaterialOrderSheetItem();
        priced.setItemId(savedItems.get(0).getItemId());
        priced.setQuantity(new BigDecimal("10"));
        priced.setPurchasePrice(new BigDecimal("68"));
        MaterialOrderSheetItem unpriced = new MaterialOrderSheetItem();
        unpriced.setItemId(savedItems.get(1).getItemId());
        unpriced.setQuantity(new BigDecimal("20"));
        unpriced.setPurchasePrice(null);

        ArgumentCaptor<PurchaseOrder> purchases = ArgumentCaptor.forClass(PurchaseOrder.class);
        ArgumentCaptor<Payable> payables = ArgumentCaptor.forClass(Payable.class);
        MaterialOrderSheet ordered = context.order(boss, currentSheet.getSheetId(), 3L, null, null, null, null,
                List.of(priced, unpriced));

        assertThat(ordered.getStatus()).isEqualTo(MaterialSheetStatus.Ordered);
        assertThat(ordered.getPriceState()).isEqualTo(SheetPriceState.Mixed);
        org.mockito.Mockito.verify(purchaseRepository, org.mockito.Mockito.atLeastOnce()).save(purchases.capture());
        PurchaseOrder header = purchases.getAllValues().get(0);
        assertThat(header.getPriceState()).as("同一供应商多行应生成一张混合价采购单").isEqualTo(SheetPriceState.Mixed);
        assertThat(header.getAmount()).isEqualByComparingTo("680");
        assertThat(savedPurchaseItems).as("采购单必须落采购明细，结算才能找到候选行").hasSize(2);
        assertThat(savedPurchaseItems).extracting(PurchaseOrderItem::getPriceState)
                .containsExactlyInAnyOrder(SheetPriceState.Priced, SheetPriceState.Unpriced);
        org.mockito.Mockito.verify(payableRepository).save(payables.capture());
        assertThat(payables.getValue().getEstimated()).as("含未定价行必须形成暂估应付").isTrue();
        assertThat(payables.getValue().getAmount()).isEqualByComparingTo("680");
    }

    @Test
    void identicalOrderRetryShouldNotCreateSecondPurchaseOrPayable() {
        MaterialPlan plan = plan(62L, new BigDecimal("10"), BigDecimal.ZERO, true);
        stubPlan(plan);
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10"), "客厅地砖")));
        currentSheet.setStatus(MaterialSheetStatus.PartiallyOrdered);

        MaterialOrderSheetItem command = new MaterialOrderSheetItem();
        command.setItemId(savedItems.get(0).getItemId());
        command.setQuantity(new BigDecimal("3"));
        command.setPurchasePrice(new BigDecimal("100"));

        context.order(boss, currentSheet.getSheetId(), 3L, null, null, null, null, List.of(command));
        context.order(boss, currentSheet.getSheetId(), 3L, null, null, null, null, List.of(command));

        assertThat(savedPurchases).as("重复请求只能生成一张采购单").hasSize(1);
        org.mockito.Mockito.verify(payableRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void pricedAndUnpricedSplitShouldProjectMixedPriceState() {
        MaterialPlan plan = plan(63L, new BigDecimal("8"), BigDecimal.ZERO, true);
        stubPlan(plan);
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("8"), "客厅地砖")));
        currentSheet.setStatus(MaterialSheetStatus.Submitted);
        Long itemId = savedItems.get(0).getItemId();

        MaterialOrderSheetItem priced = new MaterialOrderSheetItem();
        priced.setItemId(itemId);
        priced.setQuantity(new BigDecimal("3"));
        priced.setPurchasePrice(new BigDecimal("100"));
        context.order(boss, currentSheet.getSheetId(), 3L, null, null, null, null, List.of(priced));

        MaterialOrderSheetItem unpriced = new MaterialOrderSheetItem();
        unpriced.setItemId(itemId);
        unpriced.setQuantity(new BigDecimal("5"));
        unpriced.setPurchasePrice(null);
        context.order(boss, currentSheet.getSheetId(), 3L, null, null, null, null, List.of(unpriced));

        assertThat(savedPurchaseItems).as("两张采购明细").hasSize(2);
        assertThat(savedPurchaseItems).extracting(PurchaseOrderItem::getPriceState)
                .as("采购明细状态").containsExactlyInAnyOrder(SheetPriceState.Priced, SheetPriceState.Unpriced);
        assertThat(savedPurchaseItems).extracting(item -> item.getPurchaseOrder().getStatus())
                .as("采购单状态").containsOnly(PurchaseStatus.Ordered);
        assertThat(savedPurchaseItems).extracting(item -> item.getMaterialPlan().getPlanId())
                .as("采购明细计划").containsOnly(63L);
        assertThat(savedItems.get(0).getMaterialPlan().getPlanId()).as("作业单行计划").isEqualTo(63L);
        assertThat(purchaseItemRepository.findByMaterialPlan_PlanIdIn(List.of(63L)))
                .as("投影查询可读到两张采购明细").hasSize(2);
        assertThat(savedItems.get(0).getPriceState())
                .as("Priced + Unpriced 多供应商拆单必须投影为 Mixed")
                .isEqualTo(SheetPriceState.Mixed);
        assertThat(currentSheet.getPriceState()).as("作业单价格状态").isEqualTo(SheetPriceState.Mixed);
    }

    @Test
    void partialQuantityOrderShouldStayPartiallyOrdered() {
        MaterialPlan plan = plan(61L, new BigDecimal("10"), BigDecimal.ZERO, true);
        stubPlan(plan);
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10"), "客厅地砖")));
        currentSheet.setStatus(MaterialSheetStatus.Submitted);

        MaterialOrderSheetItem command = new MaterialOrderSheetItem();
        command.setItemId(savedItems.get(0).getItemId());
        command.setQuantity(new BigDecimal("5"));
        command.setPurchasePrice(new BigDecimal("68"));

        MaterialOrderSheet ordered = context.order(boss, currentSheet.getSheetId(), 3L, null, null, null, null,
                List.of(command));

        assertThat(ordered.getStatus()).isEqualTo(MaterialSheetStatus.PartiallyOrdered);
        assertThat(savedItems.get(0).getOrderedQuantity()).isEqualByComparingTo("5");
    }

    @Test
    void deleteAfterOrderedMustBeRejected() {
        MaterialPlan plan = plan(7L, new BigDecimal("10"), BigDecimal.ZERO, true);
        stubPlan(plan);
        currentSheet = context.create(designer, 9L, "Tile", null, null, null, null, null, null,
                List.of(line(plan, new BigDecimal("10"), "客厅地砖")));
        currentSheet.setStatus(MaterialSheetStatus.Ordered);

        assertThatThrownBy(() -> context.delete(designer, currentSheet.getSheetId()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能删除");
    }

    @Test
    void reportShouldUseAggregateProjection() {
        when(tenantContext.requireEmployee(any())).thenReturn(designer);
        when(sheetRepository.aggregateBySheetType(any(), any(), any())).thenReturn(List.of(
                new Object[]{"Tile", 3L, 1L, 2L},
                new Object[]{"WoodDoor", 1L, 0L, 1L}));

        var rows = context.report(designer, null);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getSheetType()).isEqualTo("Tile");
        assertThat(rows.get(0).getSheetCount()).isEqualTo(3);
        assertThat(rows.get(0).getUnpricedSheetCount())
                .as("未定价单据数＝成本待结算规模")
                .isEqualTo(1);
        assertThat(rows.get(0).getOrderedSheetCount()).isEqualTo(2);
    }

    // ------------------------------------------------------------------ 夹具

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId(role == EmployeeRole.Boss ? 2L : 1L);
        employee.setName(role.getName());
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }

    private MaterialPlan plan(Long planId, BigDecimal quantity, BigDecimal lossRate, boolean selected) {
        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(planId);
        plan.setCompany(company);
        plan.setProject(project);
        plan.setProperty(project.getProperty());
        plan.setSourceType(MaterialSourceType.Selection);
        plan.setQuantity(quantity);
        plan.setNetQuantity(quantity);
        plan.setLossRate(lossRate);
        plan.setLossQuantity(BigDecimal.ZERO);
        plan.setUnit(Unit.Sheet);
        plan.setUnitPrice(new BigDecimal("68"));
        plan.setStatus(MaterialPlanStatus.ReadyToOrder);

        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setItemId(77L);
        catalogItem.setType(ItemType.Material);
        catalogItem.setCostPrice(new BigDecimal("100"));
        catalogItem.setMaterialPrice(new BigDecimal("200"));
        catalogItem.setManualPrice(BigDecimal.ZERO);
        plan.setCatalogItem(catalogItem);

        if (selected) {
            MaterialSelection selection = new MaterialSelection();
            selection.setSelectionId(31L);
            selection.setCompany(company);
            selection.setStatus(SelectionStatus.Selected);
            plan.setSelection(selection);
        } else {
            ContractLine contractLine = new ContractLine();
            contractLine.setLineId(41L);
            contractLine.setContract(contract);
            plan.setContractLine(contractLine);
            when(selectionRepository.findFirstByCompany_ComIdAndSourceContractLine_LineIdOrderByVersionDesc(any(), any()))
                    .thenReturn(Optional.empty());
        }
        return plan;
    }

    private void stubPlan(MaterialPlan plan) {
        when(planRepository.findOne(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<MaterialPlan>>any()))
                .thenReturn(Optional.of(plan));
        // 写单/下单路径改为循环外批量取数（DEC-036 A2b）
        when(planRepository.findByProject_ProjectIdAndPlanIdIn(any(), any())).thenReturn(List.of(plan));
    }

    private MaterialOrderSheetItem line(MaterialPlan plan, BigDecimal quantity, String location) {
        MaterialOrderSheetItem item = new MaterialOrderSheetItem();
        item.setMaterialPlan(plan);
        item.setQuantity(quantity);
        item.setUnit(Unit.Sheet);
        item.setLocationName(location);
        return item;
    }
}
