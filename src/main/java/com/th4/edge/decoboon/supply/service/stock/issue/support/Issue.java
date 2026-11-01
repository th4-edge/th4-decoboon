package com.th4.edge.decoboon.supply.service.stock.issue.support;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.supply.context.InventoryContext;
import com.th4.edge.decoboon.supply.model.StockMovement;
import com.th4.edge.decoboon.supply.repository.InventoryStockRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** 项目领料出库。 */
@Note("项目领料")
@Properties(allow = {
        @AllowProperties(model = StockMovement.class,
        properties = {"movementId", "stock", "item", "project", "quoteLine", "materialPlan", "type",
                "quantity", "unitCost", "amount", "beforeQuantity", "afterQuantity", "sourceNo",
                "occurredAt", "operator", "images", "remark", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.catalog.model.CatalogItem.class, properties = {"itemId", "name", "type", "category", "brand", "series", "spec", "unit", "pricingMode", "materialPrice", "manualPrice"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.MaterialPlan.class, properties = {"planId", "unit", "quantity", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class, properties = {"projectId", "no", "name", "siteAddr", "status", "contractAmount"}),
        @AllowProperties(model = com.th4.edge.decoboon.quote.model.QuoteLine.class, properties = {"itemId", "name", "type", "category", "brand", "spec", "unit", "quantity", "price", "amount"}),
        @AllowProperties(model = com.th4.edge.decoboon.supply.model.InventoryStock.class, properties = {"stockId", "quantity", "availableQuantity", "lockedQuantity", "safetyStock", "unitCost", "batchNo"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Employee.class, properties = {"empId", "name", "role", "status"})
})
@Service("stock/issue")
@Scope(ScopeTypes.PROTOTYPE)
public class Issue implements com.th4.edge.decoboon.supply.service.stock.issue.Issue {
    @User private Employee employee;
    @AutoInject @NotNull private Long stockId;
    @AutoInject @NotNull private Long projectId;
    @AutoInject private Long quoteLineId;
    @AutoInject private Long materialPlanId;
    @AutoInject @NotNull private BigDecimal quantity;
    @AutoInject private String sourceNo;
    @AutoInject private List<String> images;
    @AutoInject private String remark;
    private final InventoryStockRepository stockRepository;
    private final ProjectRepository projectRepository;
    private final QuoteLineRepository quoteLineRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final ProjectAccessContext accessContext;
    private final InventoryContext inventoryContext;

    public Issue(InventoryStockRepository stockRepository, ProjectRepository projectRepository,
                 QuoteLineRepository quoteLineRepository, MaterialPlanRepository materialPlanRepository,
                 ProjectAccessContext accessContext, InventoryContext inventoryContext) {
        this.stockRepository = stockRepository;
        this.projectRepository = projectRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.accessContext = accessContext;
        this.inventoryContext = inventoryContext;
    }

    @Override
    @Transactional
    public StockMovement call(StockMovement request) {
        Employee current = accessContext.requireEmployee(employee);
        var stock = stockRepository.findById(stockId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "库存批次不存在")));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目不存在")));
        accessContext.requireVisible(project, current);
        var quoteLine = quoteLineId == null ? null : quoteLineRepository.findById(quoteLineId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价行不存在")));
        var plan = materialPlanId == null ? null : materialPlanRepository.findById(materialPlanId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料计划不存在")));
        return inventoryContext.issue(stock, project, quoteLine, plan, quantity, current,
                sourceNo, images, remark);
    }
}
