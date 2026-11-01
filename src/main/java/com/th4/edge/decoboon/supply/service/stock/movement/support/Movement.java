package com.th4.edge.decoboon.supply.service.stock.movement.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.decoboon.supply.model.StockMovement;
import com.th4.edge.decoboon.supply.repository.StockMovementRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

/** 库存流水分页。 */
@Note("库存流水分页")
@Properties(allow = {
        @AllowProperties(model = StockMovement.class,
                properties = {"movementId", "stock", "store", "item", "project", "quoteLine", "materialPlan",
                        "purchase", "type", "quantity", "unitCost", "amount", "beforeQuantity", "afterQuantity",
                        "sourceNo", "occurredAt", "operator", "images", "remark", "createTime"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "code", "name", "brand", "spec", "unit"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.MaterialPlan.class, properties = {"planId", "unit", "quantity", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.PurchaseOrder.class, properties = {"purchaseId", "no", "status", "supplierName", "quantity", "receivedQuantity", "unitPrice", "amount", "expectedAt"}),
        @AllowProperties(model = com.th4.edge.decoboon.quote.model.QuoteLine.class, properties = {"itemId", "name", "type", "category", "brand", "spec", "unit", "quantity", "price", "amount"}),
        @AllowProperties(model = com.th4.edge.decoboon.supply.model.InventoryStock.class, properties = {"stockId", "quantity", "availableQuantity", "lockedQuantity", "safetyStock", "unitCost", "batchNo"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Store.class, properties = {"storeId", "name", "status"})
})
@Service("stock/movement/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Movement implements com.th4.edge.decoboon.supply.service.stock.movement.Movement {
    @User private Employee employee;
    @AutoInject private Long projectId;
    private final TenantContext tenantContext;
    private final StockMovementRepository movementRepository;

    public Movement(TenantContext tenantContext, StockMovementRepository movementRepository) {
        this.tenantContext = tenantContext;
        this.movementRepository = movementRepository;
    }

    @Override
    public org.springframework.data.domain.Page<StockMovement> call(StockMovement request) {
        Employee current = tenantContext.requireEmployee(employee);
        var list = projectId == null
                ? movementRepository.findByCompany_ComIdOrderByMovementIdDesc(current.getCompany().getComId())
                : movementRepository.findByProject_ProjectIdAndCompany_ComIdOrderByMovementIdDesc(
                        projectId, current.getCompany().getComId());
        return new PageImpl<>(list.stream().map(this::safeCopy).toList());
    }

    /** 库存流水页面只需要最小业务摘要，避免完整库存/采购/项目图递归序列化。 */
    private StockMovement safeCopy(StockMovement source) {
        StockMovement copy = new StockMovement();
        BeanUtils.copyProperties(source, copy, "stock", "store", "item", "project", "quoteLine",
                "materialPlan", "purchase", "operator");
        copy.setItem(minimalItem(source.getItem()));
        copy.setProject(minimalProject(source.getProject()));
        copy.setOperator(minimalEmployee(source.getOperator()));
        return copy;
    }

    private CatalogItem minimalItem(CatalogItem source) {
        if (source == null) return null;
        CatalogItem item = new CatalogItem();
        item.setItemId(source.getItemId());
        item.setType(source.getType());
        item.setCode(source.getCode());
        item.setName(source.getName());
        item.setBrand(source.getBrand());
        item.setSpec(source.getSpec());
        item.setUnit(source.getUnit());
        return item;
    }

    private Project minimalProject(Project source) {
        if (source == null) return null;
        Project project = new Project();
        project.setProjectId(source.getProjectId());
        project.setNo(source.getNo());
        project.setName(source.getName());
        return project;
    }

    private Employee minimalEmployee(Employee source) {
        if (source == null) return null;
        Employee employee = new Employee();
        employee.setEmpId(source.getEmpId());
        employee.setName(source.getName());
        employee.setRole(source.getRole());
        return employee;
    }
}
