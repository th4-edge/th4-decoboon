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
import com.th4.edge.decoboon.project.model.PurchaseOrder;
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
                        "amount", "receivedQuantity", "status", "warehouse"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/goods-receipt/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.goodsreceipts.save.Save {
    @AutoInject @NotNull private Long purchaseId;
    @AutoInject @NotNull private BigDecimal quantity;
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

    public Save(PurchaseOrderRepository purchaseRepository,
                GoodsReceiptRepository receiptRepository,
                MaterialPlanRepository materialPlanRepository,
                PayableRepository payableRepository,
                PayableDueDateContext dueDateContext,
                ProjectAccessContext accessContext,
                InventoryContext inventoryContext,
                ModelIdentityContext modelIdentityContext,
                ProjectPermissionContext permissionContext,
                ProjectContext projectContext) {
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
        BigDecimal total = value(purchase.getQuantity());
        BigDecimal received = value(purchase.getReceivedQuantity());
        BigDecimal receivedNow = value(quantity);
        if (receivedNow.signum() <= 0 || received.add(receivedNow).compareTo(total) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "到场数量无效或超过采购数量"));
        }

        request.setCompany(purchase.getCompany());
        request.setProperty(purchase.getProperty());
        request.setProject(purchase.getProject());
        permissionContext.apply(request, purchase.getProject());
        request.setPurchase(purchase);
        request.setMaterialPlan(purchase.getMaterialPlan());
        request.setWarehouse(purchase.getWarehouse());
        request.setQuantity(receivedNow);
        request.setBatchNo(batchNo);
        request.setExpiryDate(expiryDate);
        request.setUnit(purchase.getUnit());
        request.setArrivedAt(arrivedAt == null ? LocalDateTime.now() : arrivedAt);
        request.setInspector(current);
        request.setImages(images);
        request.setRemark(remark);
        GoodsReceipt saved = receiptRepository.save(modelIdentityContext.prepare(request));

        inventoryContext.receive(purchase.getCompany(), purchase.getWarehouse(), purchase.getMaterialPlan().getCatalogItem(),
                batchNo, receivedNow, purchase.getUnitPrice(), safetyStock, expiryDate, purchase,
                purchase.getMaterialPlan(), current, purchase.getNo(), images, remark);

        BigDecimal nowReceived = received.add(receivedNow);
        purchase.setReceivedQuantity(nowReceived);
        purchase.setStatus(nowReceived.compareTo(total) >= 0 ? PurchaseStatus.Received : PurchaseStatus.PartiallyReceived);
        purchaseRepository.save(purchase);
        var plan = purchase.getMaterialPlan();
        plan.setStatus(nowReceived.compareTo(total) >= 0 ? MaterialPlanStatus.Arrived : MaterialPlanStatus.PartiallyArrived);
        plan.setArrivedAt(LocalDate.now());
        BigDecimal actualReceivedCost = receiptRepository.findByPurchaseOrderByReceiptIdDesc(purchase).stream()
                .map(item -> value(item.getQuantity()).multiply(value(purchase.getUnitPrice())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        plan.setUnitPrice(purchase.getUnitPrice());
        plan.setCost(actualReceivedCost);
        materialPlanRepository.save(plan);

        Payable payable = new Payable();
        payable.setCompany(purchase.getCompany());
        payable.setProperty(purchase.getProperty());
        payable.setProject(purchase.getProject());
        payable.setMaterialPlan(plan);
        payable.setOwner(purchase.getProperty().getOwner());
        payable.setStore(purchase.getProperty().getStore());
        payable.setTeam(purchase.getProperty().getTeam());
        payable.setName("采购到货 · " + purchase.getSupplierName());
        payable.setType(PayableType.Material);
        payable.setPayeePartner(purchase.getSupplier());
        payable.setPayeeName(purchase.getSupplierName());
        payable.setAmount(receivedNow.multiply(purchase.getUnitPrice()));
        payable.setPaidAmount(BigDecimal.ZERO);
        // 账期规则：按供应商配置的 ReceiptDays / MonthlyCutoff 计算到期日，并固化模式快照
        LocalDate receiptDate = LocalDate.now();
        payable.setReceiptDate(receiptDate);
        payable.setSettlementMode(purchase.getSupplier() == null ? null : purchase.getSupplier().getSettlementMode());
        payable.setDueDate(dueDateContext.dueDate(purchase.getSupplier(), receiptDate));
        payable.setStatus(PayableStatus.Pending);
        payable.setUrgent(false);
        payable.setSourceNo(purchase.getNo());
        payable.setRemark("材料到场自动生成应付");
        payable.setCreatedBy(current);
        payableRepository.save(modelIdentityContext.prepare(payable));
        projectContext.refreshProject(purchase.getProject());
        return saved;
    }

    private int supplierDays(PurchaseOrder purchase) {
        return purchase.getSupplier() == null ? 30 : 30;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
