package com.th4.edge.decoboon.project;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.finance.context.PayableDueDateContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.configure.DeliveryMode;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.GoodsReceipt;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.GoodsReceiptRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.supply.context.InventoryContext;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import com.th4.edge.decoboon.common.access.StoreAttributionTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 采购交付方式决定到货后是否进入库存。 */
class PurchaseDeliveryModeTest {

    private PurchaseOrderRepository purchaseRepository;
    private GoodsReceiptRepository receiptRepository;
    private MaterialPlanRepository materialPlanRepository;
    private PayableRepository payableRepository;
    private PayableDueDateContext dueDateContext;
    private ProjectAccessContext accessContext;
    private InventoryContext inventoryContext;
    private ModelIdentityContext identityContext;
    private ProjectPermissionContext permissionContext;
    private ProjectContext projectContext;
    private com.th4.edge.decoboon.project.service.goodsreceipts.save.support.Save save;

    @BeforeEach
    void setUp() {
        purchaseRepository = mock(PurchaseOrderRepository.class);
        receiptRepository = mock(GoodsReceiptRepository.class);
        materialPlanRepository = mock(MaterialPlanRepository.class);
        payableRepository = mock(PayableRepository.class);
        dueDateContext = mock(PayableDueDateContext.class);
        accessContext = mock(ProjectAccessContext.class);
        inventoryContext = mock(InventoryContext.class);
        identityContext = mock(ModelIdentityContext.class);
        permissionContext = mock(ProjectPermissionContext.class);
        projectContext = mock(ProjectContext.class);
        save = new com.th4.edge.decoboon.project.service.goodsreceipts.save.support.Save(
                purchaseRepository, receiptRepository, materialPlanRepository, payableRepository,
                dueDateContext, accessContext, inventoryContext, identityContext, permissionContext, projectContext,
                mock(com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository.class),
                new com.th4.edge.decoboon.project.context.MaterialPlanStatusContext(
                        purchaseRepository, mock(com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository.class)));
        StoreAttributionTestSupport.inject(save);
        ReflectionTestUtils.setField(save, "purchaseId", 1L);
        ReflectionTestUtils.setField(save, "quantity", new BigDecimal("5"));
        ReflectionTestUtils.setField(save, "employee", new Employee());
        when(identityContext.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(materialPlanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payableRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptRepository.findByPurchaseOrderByReceiptIdDesc(any())).thenReturn(List.of());
        when(dueDateContext.dueDate(any(), any())).thenReturn(LocalDate.now().plusDays(30));
        Specification<PurchaseOrder> visible = (root, query, builder) -> builder.conjunction();
        when(accessContext.visibleChild(any(), eq(PurchaseOrder.class), eq("project"))).thenReturn(visible);
    }

    @Test
    void directDeliveryShouldSkipInventory() {
        PurchaseOrder purchase = confirmReceipt(DeliveryMode.Direct);
        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.Received);
        verify(inventoryContext, never()).receive(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void warehouseDeliveryShouldEnterInventory() {
        PurchaseOrder purchase = confirmReceipt(DeliveryMode.Warehouse);
        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.Received);
        verify(inventoryContext).receive(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void materialPlanMustAggregateAllPurchaseReceipts() {
        PurchaseOrder existing = new PurchaseOrder();
        existing.setStatus(PurchaseStatus.Received);
        existing.setQuantity(BigDecimal.ONE);
        existing.setReceivedQuantity(BigDecimal.ONE);

        PurchaseOrder current = confirmReceipt(DeliveryMode.Direct, "2", "1", "1", List.of(existing));

        assertThat(current.getMaterialPlan().getStatus())
                .isEqualTo(com.th4.edge.decoboon.project.configure.MaterialPlanStatus.Arrived);
    }

    @Test
    void dueDateShouldUseActualArrivalDate() {
        ReflectionTestUtils.setField(save, "arrivedAt", java.time.LocalDateTime.of(2026, 9, 1, 10, 0));
        confirmReceipt(DeliveryMode.Direct);
        verify(dueDateContext).dueDate(any(), eq(LocalDate.of(2026, 9, 1)));
    }

    @Test
    void materialPlanMustRemainPartialWhenPurchasesDoNotCoverDemand() {
        PurchaseOrder current = confirmReceipt(DeliveryMode.Direct, "2", "1", "1", List.of());

        assertThat(current.getMaterialPlan().getStatus())
                .isEqualTo(com.th4.edge.decoboon.project.configure.MaterialPlanStatus.PartiallyArrived);
    }

    private PurchaseOrder confirmReceipt(DeliveryMode mode) {
        return confirmReceipt(mode, "5", "5", "5", List.of());
    }

    private PurchaseOrder confirmReceipt(DeliveryMode mode, String planQuantity, String purchaseQuantity,
                                         String receiptQuantity, List<PurchaseOrder> existingPurchases) {
        Company company = new Company();
        company.setComId(10L);
        Employee employee = new Employee();
        employee.setEmpId(20L);
        employee.setCompany(company);
        Property property = new Property();
        Project project = new Project();
        project.setProjectId(30L);
        project.setCompany(company);
        // 到场确认限定本项目项目经理/设计负责人任一方（DEC-MP-07）
        project.setManager(employee);
        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(40L);
        plan.setProject(project);
        plan.setCatalogItem(new CatalogItem());
        plan.setUnit(Unit.Item);
        plan.setQuantity(new BigDecimal(planQuantity));
        Partner supplier = new Partner();
        supplier.setPartnerId(50L);
        supplier.setCompany(company);
        supplier.setName("测试材料商");
        PurchaseOrder purchase = new PurchaseOrder();
        purchase.setPurchaseId(1L);
        purchase.setCompany(company);
        purchase.setProperty(property);
        purchase.setProject(project);
        purchase.setMaterialPlan(plan);
        purchase.setSupplier(supplier);
        purchase.setSupplierName(supplier.getName());
        purchase.setDeliveryMode(mode);
        purchase.setQuantity(new BigDecimal(purchaseQuantity));
        purchase.setReceivedQuantity(BigDecimal.ZERO);
        purchase.setUnitPrice(new BigDecimal("20"));
        purchase.setStatus(PurchaseStatus.Ordered);

        when(accessContext.requireEmployee(any())).thenReturn(employee);
        when(purchaseRepository.findOne(any(Specification.class))).thenReturn(Optional.of(purchase));
        when(purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(plan))
                .thenReturn(java.util.stream.Stream.concat(existingPurchases.stream(),
                        java.util.stream.Stream.of(purchase)).toList());
        ReflectionTestUtils.setField(save, "quantity", new BigDecimal(receiptQuantity));
        save.call(new GoodsReceipt());
        return purchase;
    }
}
