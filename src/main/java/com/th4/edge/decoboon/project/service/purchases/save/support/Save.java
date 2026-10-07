package com.th4.edge.decoboon.project.service.purchases.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.project.configure.DeliveryMode;
import com.th4.edge.decoboon.project.configure.MaterialSourceType;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.MaterialPurchaseContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.context.CarryoverLockContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
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
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 采购单创建；财务、店长和企业负责人确认后同事务形成应付。 */
@Note("采购订单创建")
@Properties(allow = {
        @AllowProperties(model = PurchaseOrder.class,
                properties = {"purchaseId", "no", "supplierName", "quantity", "unit", "unitPrice", "amount",
                        "receivedQuantity", "status", "deliveryMode", "expectedAt", "orderedAt", "approvedAt", "approvalRemark",
                        "remark", "createTime"}),
        @AllowProperties(model = Partner.class,
                properties = {"partnerId", "type", "name", "contactName", "telNo", "status"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/purchase/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.purchases.save.Save {

    @AutoInject @NotNull private Long materialPlanId;
    @AutoInject private Long supplierId;
    @AutoInject private String supplierName;
    @AutoInject @NotNull private BigDecimal quantity;
    @AutoInject @NotNull private BigDecimal unitPrice;
    @AutoInject private DeliveryMode deliveryMode;
    @AutoInject private Long warehouseId;
    @AutoInject private LocalDate expectedAt;
    @AutoInject private String remark;
    @User private Employee employee;

    private final MaterialPlanRepository materialPlanRepository;
    private final ProjectContractRepository contractRepository;
    private final PurchaseOrderRepository purchaseRepository;
    private final PartnerRepository partnerRepository;
    private final StoreRepository storeRepository;
    private final ProjectAccessContext accessContext;
    private final ModelIdentityContext modelIdentityContext;
    private final ProjectPermissionContext permissionContext;
    private final MaterialSelectionRepository materialSelectionRepository;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final CarryoverLockContext carryoverLockContext;
    private final MaterialPurchaseContext materialPurchaseContext;

    public Save(MaterialPlanRepository materialPlanRepository,
                ProjectContractRepository contractRepository,
                PurchaseOrderRepository purchaseRepository,
                PartnerRepository partnerRepository,
                StoreRepository storeRepository,
                ProjectAccessContext accessContext,
                ModelIdentityContext modelIdentityContext,
                ProjectPermissionContext permissionContext,
                MaterialSelectionRepository materialSelectionRepository,
                PurchaseOrderItemRepository purchaseItemRepository) {
        this(materialPlanRepository, contractRepository, purchaseRepository, partnerRepository, storeRepository,
                accessContext, modelIdentityContext, permissionContext, materialSelectionRepository,
                purchaseItemRepository, null, null);
    }

    @Autowired
    public Save(MaterialPlanRepository materialPlanRepository,
                ProjectContractRepository contractRepository,
                PurchaseOrderRepository purchaseRepository,
                PartnerRepository partnerRepository,
                StoreRepository storeRepository,
                ProjectAccessContext accessContext,
                ModelIdentityContext modelIdentityContext,
                ProjectPermissionContext permissionContext,
                MaterialSelectionRepository materialSelectionRepository,
                PurchaseOrderItemRepository purchaseItemRepository,
                CarryoverLockContext carryoverLockContext,
                MaterialPurchaseContext materialPurchaseContext) {
        this.materialPlanRepository = materialPlanRepository;
        this.contractRepository = contractRepository;
        this.purchaseRepository = purchaseRepository;
        this.partnerRepository = partnerRepository;
        this.storeRepository = storeRepository;
        this.accessContext = accessContext;
        this.modelIdentityContext = modelIdentityContext;
        this.permissionContext = permissionContext;
        this.materialSelectionRepository = materialSelectionRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.carryoverLockContext = carryoverLockContext;
        this.materialPurchaseContext = materialPurchaseContext;
    }

    @Override
    @Transactional
    public PurchaseOrder call(PurchaseOrder request) {
        Employee current = accessContext.requireEmployee(employee);
        materialPurchaseContext.requireOperator(current);
        Specification<com.th4.edge.decoboon.project.model.MaterialPlan> planById = (root, query, builder) ->
                builder.equal(root.get("planId"), materialPlanId);
        var plan = materialPlanRepository.findOne(planById.and(accessContext.visibleChild(
                        employee, com.th4.edge.decoboon.project.model.MaterialPlan.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料计划不存在或无权访问")));
        Project project = plan.getProject();
        if (quantity.signum() <= 0 || unitPrice.signum() < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "采购数量和单价不合法"));
        }
        materialPurchaseContext.requireOrderQuantity(plan, quantity);
        requireConfirmedSelection(plan);
        if (supplierId == null && (supplierName == null || supplierName.isBlank())) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择供应商或填写供应商名称"));
        }
        ProjectContract contract = contractRepository.findByProject(project)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "材料采购必须来源已生效合同")));
        Partner supplier = resolveSupplier(current, supplierId, supplierName);
        String priceSource = materialPurchaseContext.resolvePriceSource(plan, supplier, unitPrice);
        DeliveryMode resolvedDeliveryMode = deliveryMode == null ? DeliveryMode.Direct : deliveryMode;
        Store warehouse = resolveWarehouse(current, resolvedDeliveryMode, warehouseId);

        request.setCompany(project.getCompany());
        request.setProperty(project.getProperty());
        request.setProject(project);
        permissionContext.apply(request, project);
        request.setContract(contract);
        request.setMaterialPlan(plan);
        request.setQuoteLine(plan.getQuoteItem());
        request.setSupplier(supplier);
        request.setSupplierName(supplier.getName());
        request.setDeliveryMode(resolvedDeliveryMode);
        request.setWarehouse(warehouse);
        request.setNo("PO" + System.currentTimeMillis());
        request.setQuantity(quantity);
        request.setUnit(plan.getUnit());
        request.setUnitPrice(unitPrice);
        request.setAmount(quantity.multiply(unitPrice));
        request.setReceivedQuantity(BigDecimal.ZERO);
        request.setStatus(PurchaseStatus.Draft);
        request.setApprovedBy(current);
        request.setApprovedAt(java.time.LocalDateTime.now());
        request.setApprovalRemark("购材确认下单");
        request.setExpectedAt(expectedAt);
        request.setCreatedBy(current);
        request.setRemark(remark);
        PurchaseOrder saved = purchaseRepository.save(modelIdentityContext.prepare(request));
        saveItem(saved, plan, quantity, unitPrice, priceSource, remark);
        if (carryoverLockContext != null) carryoverLockContext.lock(project, CarryoverBusinessType.Purchase, current);
        return materialPurchaseContext.confirmOrder(saved, current);
    }

    /** 采购单明细：一采购单至少一条材料需求（多供应商拆单时同计划可有多条）。 */
    private void saveItem(PurchaseOrder purchase, com.th4.edge.decoboon.project.model.MaterialPlan plan,
                          BigDecimal quantity, BigDecimal unitPrice, String priceSource, String remark) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setCompany(purchase.getCompany());
        item.setProperty(purchase.getProperty());
        item.setProject(purchase.getProject());
        item.setContract(purchase.getContract());
        item.setPurchaseOrder(purchase);
        item.setMaterialPlan(plan);
        item.setStore(purchase.getStore());
        item.setTeam(purchase.getTeam());
        item.setOwner(purchase.getOwner());
        item.setQuantity(quantity);
        item.setUnit(purchase.getUnit());
        item.setUnitPrice(unitPrice);
        item.setAmount(quantity.multiply(unitPrice));
        item.setReceivedQuantity(BigDecimal.ZERO);
        item.setStatus(purchase.getStatus());
        item.setPriceSource(priceSource);
        item.setRemark(remark);
        purchaseItemRepository.save(modelIdentityContext.prepare(item));
    }

    /**
     * 选材确认门禁（DEC-MP-05）：合同选材来源的计划行必须存在已确认的选材结果才能下单。
     * 增减项来源的计划行在客户确认变更时生成，天然满足门禁。
     */
    private void requireConfirmedSelection(com.th4.edge.decoboon.project.model.MaterialPlan plan) {
        if (plan.getSourceType() != MaterialSourceType.Selection) {
            return;
        }
        MaterialSelection selection = plan.getSelection();
        if (selection == null && plan.getContractLine() != null && plan.getCompany() != null) {
            selection = materialSelectionRepository
                    .findFirstByCompany_ComIdAndSourceContractLine_LineIdOrderByVersionDesc(
                            plan.getCompany().getComId(), plan.getContractLine().getLineId())
                    .orElse(null);
        }
        if (selection == null || selection.getStatus() != SelectionStatus.Confirmed) {
            throw new DefinedException(new DefaultErrorCode("400", "该材料尚未完成客户选材确认，确认后方可下单"));
        }
    }

    private Partner resolveSupplier(Employee current, Long supplierId, String supplierName) {
        Long comId = current.getCompany().getComId();
        if (supplierId != null) {
            return partnerRepository.findById(supplierId)
                    .filter(item -> item.getCompany().getComId().equals(comId))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "供应商不存在")));
        }
        Partner partner = partnerRepository.findByCompany_ComIdAndName(comId, supplierName.trim());
        if (partner != null) return partner;
        partner = new Partner();
        partner.setCompany(current.getCompany());
        partner.setType(PartnerType.Supplier);
        partner.setName(supplierName.trim());
        partner.setStatus(PartnerStatus.Active);
        partner.setCreatedBy(current);
        return partnerRepository.save(modelIdentityContext.prepare(partner));
    }

    private Store resolveWarehouse(Employee current, DeliveryMode deliveryMode, Long warehouseId) {
        if (!deliveryMode.maintainsInventory()) return null;
        if (warehouseId == null) return current.getStore();
        return storeRepository.findById(warehouseId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "入库仓库不存在")));
    }
}
