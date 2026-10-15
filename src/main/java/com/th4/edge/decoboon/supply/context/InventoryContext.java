package com.th4.edge.decoboon.supply.context;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.supply.configure.StockMovementType;
import com.th4.edge.decoboon.supply.model.InventoryStock;
import com.th4.edge.decoboon.supply.model.StockMovement;
import com.th4.edge.decoboon.supply.repository.InventoryStockRepository;
import com.th4.edge.decoboon.supply.repository.StockMovementRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 库存入库、领料、损耗、退料和报价行成本回写上下文。 */
@Component
public class InventoryContext {

    private final InventoryStockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final QuoteLineRepository quoteLineRepository;
    private final ModelIdentityContext modelIdentityContext;

    public InventoryContext(InventoryStockRepository stockRepository,
                            StockMovementRepository movementRepository,
                            QuoteLineRepository quoteLineRepository,
                            ModelIdentityContext modelIdentityContext) {
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Transactional
    public StockMovement receive(Company company, Store store, CatalogItem item, String batchNo,
                                 BigDecimal quantity, BigDecimal unitCost, BigDecimal safetyStock,
                                 LocalDate expiryDate, PurchaseOrder purchase, MaterialPlan plan,
                                 Employee operator, String sourceNo, List<String> images, String remark) {
        BigDecimal qty = positive(quantity, "入库数量");
        String normalizedBatchNo = batchNo == null ? "" : batchNo.trim();
        InventoryStock stock = stockRepository
                .findFirstByCompany_ComIdAndItemAndBatchNoOrderByStockIdDesc(
                        company.getComId(), item, normalizedBatchNo);
        if (stock == null) {
            stock = new InventoryStock();
            stock.setCompany(company);
            stock.setStore(store);
            stock.setItem(item);
            stock.setBatchNo(normalizedBatchNo);
            stock.setQuantity(BigDecimal.ZERO);
            stock.setLockedQuantity(BigDecimal.ZERO);
        }
        stock.setUnitCost(unitCost == null ? BigDecimal.ZERO : unitCost);
        stock.setSafetyStock(safetyStock == null ? BigDecimal.ZERO : safetyStock);
        stock.setExpiryDate(expiryDate);
        return move(stock, purchase.getProject(), purchase.getMaterialPlan().getQuoteItem(), plan,
                purchase, StockMovementType.Inbound, qty, purchase.getUnitPrice(), operator,
                sourceNo, images, remark);
    }

    @Transactional
    public StockMovement issue(InventoryStock stock, Project project, QuoteLine quoteLine,
                               MaterialPlan plan, BigDecimal quantity, Employee operator,
                               String sourceNo, List<String> images, String remark) {
        return move(stock, project, quoteLine, plan, null, StockMovementType.Issue,
                positive(quantity, "领料数量"), stock.getUnitCost(), operator,
                sourceNo, images, remark);
    }

    @Transactional
    public StockMovement loss(InventoryStock stock, Project project, QuoteLine quoteLine,
                              MaterialPlan plan, BigDecimal quantity, Employee operator,
                              String sourceNo, List<String> images, String remark) {
        return move(stock, project, quoteLine, plan, null, StockMovementType.Loss,
                positive(quantity, "损耗数量"), stock.getUnitCost(), operator,
                sourceNo, images, remark);
    }

    @Transactional
    public StockMovement returnMaterial(InventoryStock stock, Project project, QuoteLine quoteLine,
                                        MaterialPlan plan, BigDecimal quantity, Employee operator,
                                        String sourceNo, List<String> images, String remark) {
        return move(stock, project, quoteLine, plan, null, StockMovementType.ReturnIn,
                positive(quantity, "退料数量"), stock.getUnitCost(), operator,
                sourceNo, images, remark);
    }

    @Transactional
    public StockMovement supplierReturn(InventoryStock stock, Project project, QuoteLine quoteLine,
                                        MaterialPlan plan, BigDecimal quantity, Employee operator,
                                        String sourceNo, List<String> images, String remark) {
        return move(stock, project, quoteLine, plan, null, StockMovementType.ReturnOut,
                positive(quantity, "退换数量"), stock.getUnitCost(), operator,
                sourceNo, images, remark);
    }

    public BigDecimal available(InventoryStock stock) {
        return value(stock.getQuantity()).subtract(value(stock.getLockedQuantity()));
    }

    private StockMovement move(InventoryStock stock, Project project, QuoteLine quoteLine,
                               MaterialPlan plan, PurchaseOrder purchase, StockMovementType type,
                               BigDecimal quantity, BigDecimal unitCost, Employee operator,
                               String sourceNo, List<String> images, String remark) {
        BigDecimal before = value(stock.getQuantity());
        boolean inbound = type == StockMovementType.Inbound || type == StockMovementType.ReturnIn;
        BigDecimal delta = inbound ? quantity : quantity.negate();
        if (!inbound && available(stock).compareTo(quantity) < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "库存不足，当前可用 " + available(stock)));
        }
        BigDecimal after = before.add(delta);
        BigDecimal cost = unitCost == null ? BigDecimal.ZERO : unitCost;
        BigDecimal amount = quantity.multiply(cost);
        stock.setQuantity(after);
        if (inbound) {
            stock.setLastInAt(LocalDateTime.now());
        } else {
            stock.setLastOutAt(LocalDateTime.now());
        }
        InventoryStock savedStock = stockRepository.save(modelIdentityContext.prepare(stock));

        StockMovement movement = new StockMovement();
        movement.setCompany(stock.getCompany());
        movement.setStock(savedStock);
        movement.setStore(stock.getStore());
        movement.setItem(stock.getItem());
        movement.setProject(project);
        movement.setQuoteLine(quoteLine);
        movement.setMaterialPlan(plan);
        movement.setPurchase(purchase);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setUnitCost(cost);
        movement.setAmount(amount);
        movement.setBeforeQuantity(before);
        movement.setAfterQuantity(after);
        movement.setSourceNo(sourceNo);
        movement.setOccurredAt(LocalDateTime.now());
        movement.setOperator(operator);
        movement.setImages(images);
        movement.setRemark(remark);
        movement = movementRepository.save(modelIdentityContext.prepare(movement));
        // BR-045 / DEC-072：未定价采购的金额是"未知"而非 0，禁止累计成 0 实际成本（会造成伪利润）；
        // 数量与库存事实照常登记，成本待结算价回填后再收敛。
        if (quoteLine != null && !unpriced(purchase)) {
            applyQuoteLineActualCost(quoteLine, type, amount);
        }
        return movement;
    }

    private boolean unpriced(PurchaseOrder purchase) {
        return purchase != null
                && purchase.getPriceState() == com.th4.edge.decoboon.material.configure.SheetPriceState.Unpriced;
    }

    private void applyQuoteLineActualCost(QuoteLine line, StockMovementType type, BigDecimal amount) {
        BigDecimal actual = value(line.getActualMaterialCost());
        if (type == StockMovementType.Inbound) {
            actual = actual.add(amount);
        } else if (type == StockMovementType.ReturnIn || type == StockMovementType.ReturnOut) {
            actual = actual.subtract(amount).max(BigDecimal.ZERO);
        }
        line.setActualMaterialCost(actual);
        line.setActualCost(actual
                .add(value(line.getActualLaborCost()))
                .add(value(line.getActualOtherCost())));
        quoteLineRepository.save(line);
    }

    private BigDecimal positive(BigDecimal value, String label) {
        BigDecimal result = value(value);
        if (result.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", label + "必须大于 0"));
        }
        return result;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
