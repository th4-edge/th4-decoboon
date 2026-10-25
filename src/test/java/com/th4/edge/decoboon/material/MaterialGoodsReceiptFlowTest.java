package com.th4.edge.decoboon.material;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.common.access.StoreAttributionTestSupport;
import com.th4.edge.decoboon.finance.context.PartnerCreditContext;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.material.configure.MaterialSheetStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.context.MaterialSheetContext;
import com.th4.edge.decoboon.material.context.MaterialSheetTemplateContext;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.material.repository.MaterialOrderSheetItemRepository;
import com.th4.edge.decoboon.material.repository.MaterialOrderSheetRepository;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.DeliveryMode;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.MaterialPlanStatusContext;
import com.th4.edge.decoboon.project.context.MaterialPurchaseContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.GoodsReceipt;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.GoodsReceiptRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.supply.context.InventoryContext;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 到场（验收入库）端到端定向流程（SOL-014 / OBJ-008 / BR-045）。
 *
 * <p>覆盖：有价到场写成本、**无价到场不写 0 伪成本**、部分到场分批、未下单不可验收、超量拦截，
 * 以及到场事实**单向推进作业单状态**（Ordered → PartiallyArrived → Arrived）。</p>
 */
class MaterialGoodsReceiptFlowTest {

    private final PurchaseOrderRepository purchaseRepository = mock(PurchaseOrderRepository.class);
    private final GoodsReceiptRepository receiptRepository = mock(GoodsReceiptRepository.class);
    private final MaterialPlanRepository materialPlanRepository = mock(MaterialPlanRepository.class);
    private final PayableRepository payableRepository = mock(PayableRepository.class);
    private final com.th4.edge.decoboon.finance.context.PayableDueDateContext dueDateContext =
            mock(com.th4.edge.decoboon.finance.context.PayableDueDateContext.class);
    private final ProjectAccessContext accessContext = mock(ProjectAccessContext.class);
    private final InventoryContext inventoryContext = mock(InventoryContext.class);
    private final ModelIdentityContext identityContext = mock(ModelIdentityContext.class);
    private final ProjectPermissionContext permissionContext = mock(ProjectPermissionContext.class);
    private final ProjectContext projectContext = mock(ProjectContext.class);
    private final PurchaseOrderItemRepository purchaseItemRepository = mock(PurchaseOrderItemRepository.class);
    private final MaterialPlanStatusContext planStatusContext = mock(MaterialPlanStatusContext.class);

    private final MaterialOrderSheetRepository sheetRepository = mock(MaterialOrderSheetRepository.class);
    private final MaterialOrderSheetItemRepository sheetItemRepository = mock(MaterialOrderSheetItemRepository.class);

    private MaterialSheetContext sheetContext;
    private Provider<GoodsReceipt, GoodsReceipt> save;

    private Company company;
    private Project project;
    private Employee boss;
    private MaterialPlan plan;
    private PurchaseOrder purchase;
    private PurchaseOrderItem item;
    private MaterialOrderSheet sheet;
    private MaterialOrderSheetItem sheetLine;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setComId(1L);
        boss = new Employee();
        boss.setEmpId(2L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);

        project = new Project();
        project.setProjectId(9L);
        project.setCompany(company);
        com.th4.edge.decoboon.property.model.Property property =
                new com.th4.edge.decoboon.property.model.Property();
        property.setPropertyId(7L);
        property.setCompany(company);
        project.setProperty(property);

        plan = new MaterialPlan();
        plan.setPlanId(5L);
        plan.setCompany(company);
        plan.setProject(project);
        plan.setProperty(project.getProperty());
        plan.setUnit(Unit.Sheet);
        plan.setUnitPrice(new BigDecimal("68"));
        plan.setCost(new BigDecimal("1000"));
        plan.setStatus(MaterialPlanStatus.Ordered);

        item = new PurchaseOrderItem();
        item.setPurchaseItemId(21L);
        item.setMaterialPlan(plan);
        item.setQuantity(new BigDecimal("10"));
        item.setReceivedQuantity(BigDecimal.ZERO);
        item.setUnit(Unit.Sheet);
        item.setUnitPrice(new BigDecimal("68"));

        purchase = new PurchaseOrder();
        purchase.setPurchaseId(3L);
        purchase.setCompany(company);
        purchase.setProject(project);
        purchase.setProperty(project.getProperty());
        purchase.setMaterialPlan(plan);
        purchase.setNo("PO-1");
        purchase.setSupplierName("利家福");
        purchase.setDeliveryMode(DeliveryMode.Direct);
        purchase.setQuantity(new BigDecimal("10"));
        purchase.setUnit(Unit.Sheet);
        purchase.setUnitPrice(new BigDecimal("68"));
        purchase.setAmount(new BigDecimal("680"));
        purchase.setReceivedQuantity(BigDecimal.ZERO);
        purchase.setStatus(PurchaseStatus.Ordered);
        purchase.setPriceState(SheetPriceState.Priced);

        sheet = new MaterialOrderSheet();
        sheet.setSheetId(1000L);
        sheet.setCompany(company);
        sheet.setProject(project);
        sheet.setSheetType("Tile");
        sheet.setStatus(MaterialSheetStatus.Ordered);
        sheetLine = new MaterialOrderSheetItem();
        sheetLine.setItemId(500L);
        sheetLine.setSheet(sheet);
        sheetLine.setMaterialPlan(plan);
        sheetLine.setQuantity(new BigDecimal("10"));
        sheetLine.setOrderedQuantity(new BigDecimal("10"));
        sheetLine.setUnit(Unit.Sheet);

        sheetContext = new MaterialSheetContext(sheetRepository, sheetItemRepository, materialPlanRepository,
                mock(ProjectContractRepository.class), purchaseRepository, purchaseItemRepository,
                mock(PartnerRepository.class), accessContext, permissionContext, identityContext, mock(TenantContext.class),
                StoreAttributionTestSupport.inject(new MaterialPurchaseContext(purchaseRepository,
                        purchaseItemRepository, materialPlanRepository, planStatusContext, payableRepository,
                        identityContext, mock(PartnerCreditContext.class))),
                new MaterialSelectionContext(mock(MaterialSelectionRepository.class), null, null, null, null, null, null),
                mock(MaterialSheetTemplateContext.class), null);

        save = new com.th4.edge.decoboon.project.service.goodsreceipts.save.support.Save(
                purchaseRepository, receiptRepository, materialPlanRepository, payableRepository, dueDateContext,
                accessContext, inventoryContext, identityContext, permissionContext, projectContext,
                purchaseItemRepository, planStatusContext, sheetContext);
        StoreAttributionTestSupport.inject(save);
        ReflectionTestUtils.setField(save, "employee", boss);
        ReflectionTestUtils.setField(save, "purchaseId", 3L);

        when(identityContext.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accessContext.requireEmployee(any())).thenReturn(boss);
        when(accessContext.visibleChild(any(), any(), any())).thenReturn((root, query, builder) -> builder.conjunction());
        when(purchaseRepository.findOne(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<PurchaseOrder>>any()))
                .thenReturn(Optional.of(purchase));
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(any())).thenReturn(List.of(item));
        when(purchaseItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        // 到场记录需被"按采购单累计成本"读回，否则计划成本会被算成 0
        final java.util.List<GoodsReceipt> savedReceipts = new java.util.ArrayList<>();
        when(receiptRepository.save(any())).thenAnswer(invocation -> {
            GoodsReceipt receipt = invocation.getArgument(0);
            if (receipt != null && receipt.getReceiptId() == null) receipt.setReceiptId(700L);
            savedReceipts.add(receipt);
            return receipt;
        });
        when(receiptRepository.findByPurchaseOrderByReceiptIdDesc(any()))
                .thenAnswer(invocation -> new java.util.ArrayList<>(savedReceipts));
        when(materialPlanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(planStatusContext.resolve(any())).thenReturn(MaterialPlanStatus.PartiallyArrived);
        when(payableRepository.findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(any(), any()))
                .thenReturn(Optional.empty());
        when(payableRepository.findFirstByCompany_ComIdAndSourceNoAndType(any(), any(), any()))
                .thenReturn(Optional.empty());
        // 作业单回写链路
        when(purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(any())).thenReturn(List.of(purchase));
        when(purchaseItemRepository.findByMaterialPlanOrderByPurchaseItemIdAsc(any())).thenReturn(List.of(item));
        when(sheetItemRepository.findByMaterialPlan_PlanId(any())).thenReturn(List.of(sheetLine));
        when(sheetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void receiptOf(String quantity) {
        ReflectionTestUtils.setField(save, "quantity", new BigDecimal(quantity));
        save.call(new GoodsReceipt());
    }

    @Test
    void pricedArrivalShouldWriteCostAndAdvanceSheet() {
        receiptOf("10");

        assertThat(purchase.getReceivedQuantity()).isEqualByComparingTo("10");
        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.Received);
        assertThat(item.getStatus()).isEqualTo(PurchaseStatus.Received);
        assertThat(plan.getUnitPrice()).isEqualByComparingTo("68");
        assertThat(plan.getCost()).isEqualByComparingTo("680");
        assertThat(sheet.getStatus())
                .as("到场事实必须单向推进作业单状态（OBJ-008 映射表）")
                .isEqualTo(MaterialSheetStatus.Arrived);
    }

    @Test
    void unpricedArrivalMustNotWriteZeroCost() {
        purchase.setPriceState(SheetPriceState.Unpriced);
        purchase.setUnitPrice(BigDecimal.ZERO);
        purchase.setAmount(BigDecimal.ZERO);
        item.setUnitPrice(BigDecimal.ZERO);

        receiptOf("10");

        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.Received);
        assertThat(plan.getCost())
                .as("无价到场不得把 0 当作真实成本写回计划行（否则会算出伪利润）")
                .isEqualByComparingTo("1000");
        assertThat(plan.getUnitPrice()).isEqualByComparingTo("68");
        assertThat(sheet.getStatus()).isEqualTo(MaterialSheetStatus.Arrived);
    }

    @Test
    void partialArrivalShouldAdvanceToPartiallyArrivedThenArrived() {
        receiptOf("4");
        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.PartiallyReceived);
        assertThat(sheet.getStatus()).isEqualTo(MaterialSheetStatus.PartiallyArrived);

        receiptOf("6");
        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.Received);
        assertThat(sheet.getStatus()).isEqualTo(MaterialSheetStatus.Arrived);
    }

    @Test
    void arrivalBeforeOrderingMustBeRejected() {
        purchase.setStatus(PurchaseStatus.Approved);
        assertThatThrownBy(() -> receiptOf("10"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("采购单下单后才能验收入库");
    }

    @Test
    void arrivalQuantityAboveOrderMustBeRejected() {
        assertThatThrownBy(() -> receiptOf("11"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("到场数量无效或超过采购数量");
    }

    @Test
    void arrivalShouldPersistReceiptWithPurchaseLink() {
        ArgumentCaptor<GoodsReceipt> captor = ArgumentCaptor.forClass(GoodsReceipt.class);
        receiptOf("10");
        org.mockito.Mockito.verify(receiptRepository).save(captor.capture());
        GoodsReceipt receipt = captor.getValue();
        assertThat(receipt.getPurchase()).isEqualTo(purchase);
        assertThat(receipt.getQuantity()).isEqualByComparingTo("10");
        assertThat(receipt.getMaterialPlan()).isEqualTo(plan);
    }
}
