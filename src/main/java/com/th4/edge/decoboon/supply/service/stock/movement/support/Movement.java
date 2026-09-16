package com.th4.edge.decoboon.supply.service.stock.movement.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
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
                properties = {"itemId", "type", "code", "name", "brand", "spec", "unit"})
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
        return new PageImpl<>(list);
    }
}
