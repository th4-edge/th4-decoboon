package com.th4.edge.decoboon.project.service.goodsreceipts.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.context.PayableDueDateContext;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.GoodsReceipt;
import com.th4.edge.decoboon.project.context.MaterialPlanStatusContext;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.GoodsReceiptRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.supply.context.InventoryContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 材料到场、入库、库存流水与应付联动。 */
@Note("材料到场入库")
@Properties(allow = {
        @AllowProperties(model = GoodsReceipt.class,
                properties = {"receiptId", "quantity", "batchNo", "expiryDate", "unit", "arrivedAt",
                        "images", "remark", "createTime"}),
        @AllowProperties(model = PurchaseOrder.class,
                properties = {"purchaseId", "no", "supplier", "supplierName", "quantity", "unitPrice",
                        "amount", "receivedQuantity", "status", "deliveryMode", "warehouse"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/goods-receipt/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.goodsreceipts.save.Save {
    @AutoInject @NotNull private Long purchaseId;
    @AutoInject @NotNull private BigDecimal quantity;
    @AutoInject private Long purchaseItemId;
    @AutoInject private String batchNo;
    @AutoInject private LocalDate expiryDate;
    @AutoInject private BigDecimal safetyStock;
    @AutoInject private LocalDateTime arrivedAt;
    @AutoInject private List<String> images;
    @AutoInject private String remark;
    @User private Employee employee;

    private final PurchaseOrderRepository purchaseRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final PayableRepository payableRepository;
    private final PayableDueDateContext dueDateContext;
    private final ProjectAccessContext accessContext;
    private final InventoryContext inventoryContext;
    private final ModelIdentityContext modelIdentityContext;
    private final ProjectPermissionContext permissionContext;
    private final ProjectContext projectContext;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final MaterialPlanStatusContext materialPlanStatusContext;

    public Save(PurchaseOrderRepository purchaseRepository,
                GoodsReceiptRepository receiptRepository,
                MaterialPlanRepository materialPlanRepository,
                PayableRepository payableRepository,
                PayableDueDateContext dueDateContext,
                ProjectAccessContext accessContext,
                InventoryContext inventoryContext,
                ModelIdentityContext modelIdentityContext,
                ProjectPermissionContext permissionContext,
                ProjectContext projectContext,
                PurchaseOrderItemRepository purchaseItemRepository,
                MaterialPlanStatusContext materialPlanStatusContext) {
        this.purchaseRepository = purchaseRepository;
        this.receiptRepository = receiptRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.payableRepository = payableRepository;
        this.dueDateContext = dueDateContext;
        this.accessContext = accessContext;
        this.inventoryContext = inventoryContext;
        this.modelIdentityContext = modelIdentityContext;
        this.permissionContext = permissionContext;
        this.projectContext = projectContext;
        this.purchaseItemRepository = purchaseItemRepository;
        this.materialPlanStatusContext = materialPlanStatusContext;
    }

    @Override
    @Transactional
    public GoodsReceipt call(GoodsReceipt request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<PurchaseOrder> byId = (root, query, builder) -> builder.equal(root.get("purchaseId"), purchaseId);
        PurchaseOrder purchase = purchaseRepository.findOne(
                        byId.and(accessContext.visibleChild(employee, PurchaseOrder.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "采购订单不存在或无权访问")));
        if (purchase.getStatus() != PurchaseStatus.Ordered
                && purchase.getStatus() != PurchaseStatus.PartiallyReceived) {
            throw new DefinedException(new DefaultErrorCode("400", "采购单下单后才能验收入库"));
        }
        requireArrivalConfirmer(current, purchase);
        java.util.List<PurchaseOrderItem> items =
                purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(purchase);
        PurchaseOrderItem item = resolveItem(items);
        BigDecimal total = item != null ? value(item.getQuantity()) : value(purchase.getQuantity());
        BigDecimal received = item != null ? value(item.getReceivedQuantity()) : value(purchase.getReceivedQuantity());
        BigDecimal receivedNow = value(quantity);
        if (receivedNow.signum() <= 0 || received.add(receivedNow).compareTo(total) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "到场数量无效或超过采购数量"));
        }
        var plan = item != null ? item.getMaterialPlan() : purchase.getMaterialPlan();
        BigDecimal unitPrice = item != null ? value(item.getUnitPrice()) : value(purchase.getUnitPrice());

        request.setCompany(purchase.getCompany());
        request.setProperty(purchase.getProperty());
        request.setProject(purchase.getProject());
        permissionContext.apply(request, purchase.getProject());
        request.setPurchase(purchase);
        request.setPurchaseItem(item);
        request.setMaterialPlan(plan);
        request.setWarehouse(purchase.getWarehouse());
        request.setQuantity(receivedNow);
        request.setBatchNo(batchNo);
        request.setExpiryDate(expiryDate);
        request.setUnit(item != null ? item.getUnit() : purchase.getUnit());
        request.setArrivedAt(arrivedAt == null ? LocalDateTime.now() : arrivedAt);
        request.setInspector(current);
        request.setImages(images);
        request.setRemark(remark);
        GoodsReceipt saved = receiptRepository.save(modelIdentityContext.prepare(request));

        if (purchase.getDeliveryMode() != null && purchase.getDeliveryMode().maintainsInventory()) {
            inventoryContext.receive(purchase.getCompany(), purchase.getWarehouse(),
                    plan.getCatalogItem(), batchNo, receivedNow, unitPrice, safetyStock, expiryDate,
                    purchase, plan, current, purchase.getNo(), images, remark);
        }

        BigDecimal nowReceived = received.add(receivedNow);
        if (item != null) {
            item.setReceivedQuantity(nowReceived);
            item.setStatus(nowReceived.compareTo(total) >= 0
                    ? PurchaseStatus.Received : PurchaseStatus.PartiallyReceived);
            purchaseItemRepository.save(item);
            BigDecimal headerReceived = purchaseItemRepository
                    .findByPurchaseOrderOrderByPurchaseItemIdAsc(purchase).stream()
                    .map(row -> value(row.getReceivedQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            purchase.setReceivedQuantity(headerReceived);
            purchase.setStatus(headerReceived.compareTo(value(purchase.getQuantity())) >= 0
                    ? PurchaseStatus.Received : PurchaseStatus.PartiallyReceived);
        } else {
            purchase.setReceivedQuantity(nowReceived);
            purchase.setStatus(nowReceived.compareTo(total) >= 0
                    ? PurchaseStatus.Received : PurchaseStatus.PartiallyReceived);
        }
        purchaseRepository.save(purchase);

        plan.setStatus(materialPlanStatusContext.resolve(plan));
        plan.setArrivedAt(LocalDate.now());
        BigDecimal actualReceivedCost = receiptRepository.findByPurchaseOrderByReceiptIdDesc(purchase).stream()
                .map(row -> value(row.getQuantity()).multiply(unitPrice))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        plan.setUnitPrice(unitPrice);
        plan.setCost(actualReceivedCost);
        materialPlanRepository.save(plan);

        backfillPayableDueDate(purchase);
        projectContext.refreshProject(purchase.getProject());
        return saved;
    }

    /** 解析到场对应的采购明细：单明细自动匹配，多明细必须显式指定。 */
    private PurchaseOrderItem resolveItem(java.util.List<PurchaseOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        if (purchaseItemId == null) {
            if (items.size() == 1) {
                return items.get(0);
            }
            throw new DefinedException(new DefaultErrorCode("400",
                    "该采购单包含多条材料明细，请指定采购明细后再登记到场"));
        }
        return items.stream()
                .filter(row -> purchaseItemId.equals(row.getPurchaseItemId()))
                .findFirst()
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "采购明细不存在")));
    }

    /**
     * 到场确认人限定为本项目项目经理或设计负责人任一方（DEC-MP-07）；
     * 企业负责人/财务作为兜底仍可操作。
     */
    private void requireArrivalConfirmer(Employee current, PurchaseOrder purchase) {
        var project = purchase.getProject();
        boolean isProjectPm = project != null && project.getManager() != null
                && current.getEmpId() != null && current.getEmpId().equals(project.getManager().getEmpId());
        boolean isProjectDesigner = project != null && project.getDesigner() != null
                && current.getEmpId() != null && current.getEmpId().equals(project.getDesigner().getEmpId());
        boolean fallback = current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.Finance;
        if (!isProjectPm && !isProjectDesigner && !fallback) {
            throw new DefinedException(new DefaultErrorCode("403",
                    "材料到场确认需由本项目项目经理或设计负责人完成"));
        }
    }

    /**
     * DEC-MP-01：应付在下单时已形成，到场确认只回填账期起算日与实际到期日。
     * 历史采购单若无应付，则按采购金额补建，保证应付不缺失。
     */
    private void backfillPayableDueDate(PurchaseOrder purchase) {
        LocalDate receiptDate = LocalDate.now();
        Long comId = purchase.getCompany().getComId();
        var existing = payableRepository
                .findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(comId, purchase.getPurchaseId());
        if (existing.isEmpty()) {
            existing = payableRepository.findFirstByCompany_ComIdAndSourceNoAndType(
                    comId, purchase.getNo(), PayableType.Material);
        }
        if (existing.isPresent()) {
            Payable payable = existing.get();
            if (payable.getReceiptDate() == null) {
                payable.setReceiptDate(receiptDate);
                payable.setSettlementMode(purchase.getSupplier() == null
                        ? null : purchase.getSupplier().getSettlementMode());
                payable.setDueDate(dueDateContext.dueDate(purchase.getSupplier(), receiptDate));
                payableRepository.save(payable);
            }
            return;
        }
        Payable payable = new Payable();
        payable.setCompany(purchase.getCompany());
        payable.setProperty(purchase.getProperty());
        payable.setProject(purchase.getProject());
        payable.setMaterialPlan(purchase.getMaterialPlan());
        payable.setPurchaseOrder(purchase);
        payable.setOwner(resolveOwner(purchase));
        payable.setStore(purchase.getProperty().getStore());
        payable.setTeam(purchase.getProperty().getTeam());
        payable.setName("材料采购 · " + purchase.getSupplierName());
        payable.setType(PayableType.Material);
        payable.setPayeePartner(purchase.getSupplier());
        payable.setPayeeName(purchase.getSupplierName());
        payable.setAmount(value(purchase.getAmount()));
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setReceiptDate(receiptDate);
        payable.setSettlementMode(purchase.getSupplier() == null ? null : purchase.getSupplier().getSettlementMode());
        payable.setDueDate(dueDateContext.dueDate(purchase.getSupplier(), receiptDate));
        payable.setStatus(PayableStatus.Pending);
        payable.setUrgent(false);
        payable.setSourceNo(purchase.getNo());
        payable.setRemark("历史采购单到场补建应付");
        payable.setCreatedBy(purchase.getCreatedBy());
        payableRepository.save(modelIdentityContext.prepare(payable));
    }

    private Employee resolveOwner(PurchaseOrder purchase) {
        if (purchase.getProperty() != null && purchase.getProperty().getOwner() != null) {
            return purchase.getProperty().getOwner();
        }
        return purchase.getCreatedBy();
    }


    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
