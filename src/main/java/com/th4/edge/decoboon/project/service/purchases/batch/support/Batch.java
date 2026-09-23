package com.th4.edge.decoboon.project.service.purchases.batch.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.DeliveryMode;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.MaterialSourceType;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
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
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 按供应商合并下单：同一供应商的多条材料需求生成一张采购单（多条明细）。
 *
 * <p>同一条材料需求也可拆给多个供应商——调用方分多次提交即可，每次形成一张采购单与一条明细。</p>
 */
@Note("采购单合并下单")
@Properties(allow = {
        @AllowProperties(model = PurchaseOrder.class,
                properties = {"purchaseId", "no", "supplierName", "quantity", "unit", "unitPrice", "amount",
                        "receivedQuantity", "status", "deliveryMode", "expectedAt", "orderedAt",
                        "approvedAt", "approvalRemark", "remark", "createTime", "supplier"}),
        @AllowProperties(model = com.th4.edge.decoboon.supply.model.Partner.class,
                properties = {"partnerId", "type", "name", "contactName", "telNo", "status"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/purchase/batch")
@Scope(ScopeTypes.PROTOTYPE)
public class Batch implements com.th4.edge.decoboon.project.service.purchases.batch.Batch {

    @AutoInject @NotNull private Long supplierId;
    @AutoInject private String supplierName;
    @AutoInject @NotEmpty private List<Long> materialPlanIds;
    @AutoInject private List<BigDecimal> quantities;
    @AutoInject private List<BigDecimal> unitPrices;
    @AutoInject private DeliveryMode deliveryMode;
    @AutoInject private Long warehouseId;
    @AutoInject private LocalDate expectedAt;
    @AutoInject private String remark;
    @User private Employee employee;

    private final MaterialPlanRepository materialPlanRepository;
    private final PurchaseOrderRepository purchaseRepository;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final ProjectContractRepository contractRepository;
    private final PartnerRepository partnerRepository;
    private final MaterialSelectionRepository materialSelectionRepository;
    private final StoreRepository storeRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectPermissionContext permissionContext;
    private final ModelIdentityContext modelIdentityContext;

    public Batch(MaterialPlanRepository materialPlanRepository,
                 PurchaseOrderRepository purchaseRepository,
                 PurchaseOrderItemRepository purchaseItemRepository,
                 ProjectContractRepository contractRepository,
                 PartnerRepository partnerRepository,
                 MaterialSelectionRepository materialSelectionRepository,
                 StoreRepository storeRepository,
                 ProjectAccessContext accessContext,
                 ProjectPermissionContext permissionContext,
                 ModelIdentityContext modelIdentityContext) {
        this.materialPlanRepository = materialPlanRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.contractRepository = contractRepository;
        this.partnerRepository = partnerRepository;
        this.materialSelectionRepository = materialSelectionRepository;
        this.storeRepository = storeRepository;
        this.accessContext = accessContext;
        this.permissionContext = permissionContext;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Override
    @Transactional
    public PurchaseOrder call(PurchaseOrder request) {
        Employee current = accessContext.requireEmployee(employee);
        if (quantities == null || quantities.size() != materialPlanIds.size()) {
            throw new DefinedException(new DefaultErrorCode("400", "材料与数量必须一一对应"));
        }
        if (unitPrices == null || unitPrices.size() != materialPlanIds.size()) {
            throw new DefinedException(new DefaultErrorCode("400", "材料与单价必须一一对应"));
        }
        List<MaterialPlan> plans = new ArrayList<>();
        Project project = null;
        for (Long planId : materialPlanIds) {
            Specification<MaterialPlan> byId = (root, query, builder) -> builder.equal(root.get("planId"), planId);
            MaterialPlan plan = materialPlanRepository.findOne(byId.and(accessContext.visibleChild(
                            employee, MaterialPlan.class, "project")))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料计划不存在或无权访问")));
            if (project == null) {
                project = plan.getProject();
            } else if (!project.getProjectId().equals(plan.getProject().getProjectId())) {
                throw new DefinedException(new DefaultErrorCode("400", "合并下单的材料必须属于同一项目"));
            }
            requireOrderable(plan);
            plans.add(plan);
        }
        ProjectContract contract = contractRepository.findByProject(project)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "材料采购必须来源已生效合同")));
        Partner supplier = resolveSupplier(current, supplierId, supplierName);
        DeliveryMode resolvedMode = deliveryMode == null ? DeliveryMode.Direct : deliveryMode;
        Store warehouse = resolveWarehouse(current, resolvedMode, warehouseId);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (int i = 0; i < plans.size(); i++) {
            BigDecimal quantity = quantities.get(i);
            BigDecimal unitPrice = unitPrices.get(i);
            if (quantity == null || quantity.signum() <= 0 || unitPrice == null || unitPrice.signum() < 0) {
                throw new DefinedException(new DefaultErrorCode("400", "采购数量和单价不合法"));
            }
            totalAmount = totalAmount.add(quantity.multiply(unitPrice));
            totalQuantity = totalQuantity.add(quantity);
        }

        request.setCompany(project.getCompany());
        request.setProperty(project.getProperty());
        request.setProject(project);
        permissionContext.apply(request, project);
        request.setContract(contract);
        request.setMaterialPlan(plans.get(0));
        request.setSupplier(supplier);
        request.setSupplierName(supplier.getName());
        request.setQuoteLine(plans.get(0).getQuoteItem());
        request.setDeliveryMode(resolvedMode);
        request.setWarehouse(warehouse);
        request.setNo("PO" + System.currentTimeMillis());
        request.setQuantity(totalQuantity);
        request.setUnit(plans.get(0).getUnit());
        request.setUnitPrice(BigDecimal.ZERO);
        request.setAmount(totalAmount);
        request.setReceivedQuantity(BigDecimal.ZERO);
        boolean autoApproved = current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.Finance;
        request.setStatus(autoApproved ? PurchaseStatus.Approved : PurchaseStatus.Submitted);
        request.setApprovedBy(autoApproved ? current : null);
        request.setApprovedAt(autoApproved ? java.time.LocalDateTime.now() : null);
        request.setApprovalRemark(autoApproved ? "企业负责人/财务直接确认" : null);
        request.setExpectedAt(expectedAt);
        request.setCreatedBy(current);
        request.setRemark(remark);
        PurchaseOrder saved = purchaseRepository.save(modelIdentityContext.prepare(request));

        for (int i = 0; i < plans.size(); i++) {
            MaterialPlan plan = plans.get(i);
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setCompany(saved.getCompany());
            item.setProperty(saved.getProperty());
            item.setProject(saved.getProject());
            item.setContract(saved.getContract());
            item.setPurchaseOrder(saved);
            item.setMaterialPlan(plan);
            item.setStore(saved.getStore());
            item.setTeam(saved.getTeam());
            item.setOwner(saved.getOwner());
            item.setQuantity(quantities.get(i));
            item.setUnit(plan.getUnit());
            item.setUnitPrice(unitPrices.get(i));
            item.setAmount(quantities.get(i).multiply(unitPrices.get(i)));
            item.setReceivedQuantity(BigDecimal.ZERO);
            item.setStatus(saved.getStatus());
            item.setPriceSource("财务下单录入");
            purchaseItemRepository.save(modelIdentityContext.prepare(item));

            plan.setStatus(MaterialPlanStatus.Requested);
            materialPlanRepository.save(plan);
        }
        return saved;
    }

    private void requireOrderable(MaterialPlan plan) {
        MaterialPlanStatus status = plan.getStatus();
        if (status == MaterialPlanStatus.Cancelled || status == MaterialPlanStatus.Closed
                || status == MaterialPlanStatus.Arrived) {
            throw new DefinedException(new DefaultErrorCode("400", "该材料当前状态不能下单"));
        }
        if (plan.getSourceType() == MaterialSourceType.Selection) {
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
    }

    private Partner resolveSupplier(Employee current, Long partnerId, String name) {
        Long comId = current.getCompany().getComId();
        if (partnerId != null) {
            return partnerRepository.findById(partnerId)
                    .filter(item -> item.getCompany().getComId().equals(comId))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "供应商不存在")));
        }
        if (name == null || name.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择供应商或填写供应商名称"));
        }
        Partner partner = partnerRepository.findByCompany_ComIdAndName(comId, name.trim());
        if (partner != null) {
            return partner;
        }
        partner = new Partner();
        partner.setCompany(current.getCompany());
        partner.setType(PartnerType.Supplier);
        partner.setName(name.trim());
        partner.setStatus(PartnerStatus.Active);
        partner.setCreatedBy(current);
        return partnerRepository.save(modelIdentityContext.prepare(partner));
    }

    private Store resolveWarehouse(Employee current, DeliveryMode mode, Long warehouseId) {
        if (!mode.maintainsInventory()) {
            return null;
        }
        if (warehouseId == null) {
            return current.getStore();
        }
        return storeRepository.findById(warehouseId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "入库仓库不存在")));
    }
}
