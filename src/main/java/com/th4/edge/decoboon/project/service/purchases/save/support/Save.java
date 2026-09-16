package com.th4.edge.decoboon.project.service.purchases.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
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
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 采购单创建，提交后进入审批。 */
@Note("采购订单创建")
@Properties(allow = {
        @AllowProperties(model = PurchaseOrder.class,
                properties = {"purchaseId", "no", "property", "project", "contract", "materialPlan",
                        "supplier", "supplierName", "quoteLine", "warehouse", "quantity", "unit",
                        "unitPrice", "amount", "receivedQuantity", "status", "expectedAt", "orderedAt",
                        "createdBy", "approvedBy", "approvedAt", "approvalRemark", "remark", "createTime"}),
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

    public Save(MaterialPlanRepository materialPlanRepository,
                ProjectContractRepository contractRepository,
                PurchaseOrderRepository purchaseRepository,
                PartnerRepository partnerRepository,
                StoreRepository storeRepository,
                ProjectAccessContext accessContext,
                ModelIdentityContext modelIdentityContext,
                ProjectPermissionContext permissionContext) {
        this.materialPlanRepository = materialPlanRepository;
        this.contractRepository = contractRepository;
        this.purchaseRepository = purchaseRepository;
        this.partnerRepository = partnerRepository;
        this.storeRepository = storeRepository;
        this.accessContext = accessContext;
        this.modelIdentityContext = modelIdentityContext;
        this.permissionContext = permissionContext;
    }

    @Override
    @Transactional
    public PurchaseOrder call(PurchaseOrder request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<com.th4.edge.decoboon.project.model.MaterialPlan> planById = (root, query, builder) ->
                builder.equal(root.get("planId"), materialPlanId);
        var plan = materialPlanRepository.findOne(planById.and(accessContext.visibleChild(
                        employee, com.th4.edge.decoboon.project.model.MaterialPlan.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料计划不存在或无权访问")));
        Project project = plan.getProject();
        if (quantity.signum() <= 0 || unitPrice.signum() < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "采购数量和单价不合法"));
        }
        if (supplierId == null && (supplierName == null || supplierName.isBlank())) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择供应商或填写供应商名称"));
        }
        ProjectContract contract = contractRepository.findByProject(project)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "材料采购必须来源已生效合同")));
        Partner supplier = resolveSupplier(current, supplierId, supplierName);
        Store warehouse = resolveWarehouse(current, warehouseId);

        request.setCompany(project.getCompany());
        request.setProperty(project.getProperty());
        request.setProject(project);
        permissionContext.apply(request, project);
        request.setContract(contract);
        request.setMaterialPlan(plan);
        request.setQuoteLine(plan.getQuoteItem());
        request.setSupplier(supplier);
        request.setSupplierName(supplier.getName());
        request.setWarehouse(warehouse);
        request.setNo("PO" + System.currentTimeMillis());
        request.setQuantity(quantity);
        request.setUnit(plan.getUnit());
        request.setUnitPrice(unitPrice);
        request.setAmount(quantity.multiply(unitPrice));
        request.setReceivedQuantity(BigDecimal.ZERO);
        request.setStatus(PurchaseStatus.Submitted);
        request.setExpectedAt(expectedAt);
        request.setCreatedBy(current);
        request.setRemark(remark);
        plan.setStatus(MaterialPlanStatus.Requested);
        materialPlanRepository.save(plan);
        return purchaseRepository.save(modelIdentityContext.prepare(request));
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

    private Store resolveWarehouse(Employee current, Long warehouseId) {
        if (warehouseId == null) return current.getStore();
        return storeRepository.findById(warehouseId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "入库仓库不存在")));
    }
}
