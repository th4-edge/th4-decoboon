package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 合同变更金额与成本计算的唯一口径。
 *
 * <p>单条发起与批量发起都必须经过本组件，禁止由前端直接决定净变更金额：
 * 有原合同行时按原数量/原单价与新数量/新单价服务端计算；无原合同行的增项/减项才允许手填金额。
 * 原合同行按合同范围批量加载，避免逐行查库。</p>
 */
@Component
public class ContractChangeAmountContext {

    private final ContractLineRepository lineRepository;

    public ContractChangeAmountContext(ContractLineRepository lineRepository) {
        this.lineRepository = lineRepository;
    }

    /** 单条发起：按合同范围取原合同行。 */
    public ContractLine line(ProjectContract contract, Long lineId) {
        if (lineId == null) return null;
        return lineRepository.findOne((root, query, builder) -> builder.and(
                        builder.equal(root.get("lineId"), lineId),
                        builder.equal(root.get("contract").get("contractId"), contract.getContractId())))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "原合同行不存在")));
    }

    /** 批量发起：一次查询按合同范围加载原合同行，避免逐行查库。 */
    public Map<Long, ContractLine> lines(ProjectContract contract, Collection<Long> lineIds) {
        Map<Long, ContractLine> map = new LinkedHashMap<>();
        if (lineIds == null || lineIds.isEmpty()) return map;
        for (ContractLine line : lineRepository.findByLineIdInAndContract_ContractId(lineIds, contract.getContractId())) {
            map.put(line.getLineId(), line);
        }
        return map;
    }

    /** 计算原数量/新数量/原单价/新单价/金额变化/成本变化，供单条与批量共用。 */
    public Computed compute(ContractChangeType type, ContractLine source, BigDecimal requestedAmount,
                            BigDecimal requestedCost, BigDecimal newQuantityValue,
                            BigDecimal newPriceValue, BigDecimal newCostValue) {
        BigDecimal oldQuantity = source == null ? BigDecimal.ZERO : value(source.getQuantity());
        BigDecimal oldPrice = source == null || oldQuantity.signum() == 0
                ? BigDecimal.ZERO : value(source.getAmount()).divide(oldQuantity, 2, RoundingMode.HALF_UP);
        BigDecimal newQuantity = newQuantityValue == null ? oldQuantity : newQuantityValue;
        BigDecimal newPrice = newPriceValue == null ? oldPrice : newPriceValue;
        BigDecimal amountDelta = amountDelta(type, source, requestedAmount, newQuantityValue, newPriceValue);
        BigDecimal costDelta = costDelta(type, source, requestedCost, newQuantityValue, newPriceValue, newCostValue);
        return new Computed(oldQuantity, newQuantity, oldPrice, newPrice, amountDelta, costDelta);
    }

    public BigDecimal amountDelta(ContractChangeType type, ContractLine source, BigDecimal requested,
                                  BigDecimal newQuantity, BigDecimal newPrice) {
        if (requested != null) return requested;
        if (source == null) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少原合同行，无法计算变更金额"));
        }
        if (type == ContractChangeType.Quantity || type == ContractChangeType.Replace) {
            BigDecimal quantity = newQuantity == null ? source.getQuantity() : newQuantity;
            BigDecimal price = newPrice != null ? newPrice : unitPrice(source);
            return quantity.subtract(value(source.getQuantity())).multiply(price);
        }
        if (type == ContractChangeType.Price) {
            BigDecimal price = newPrice == null ? unitPrice(source) : newPrice;
            return price.subtract(unitPrice(source)).multiply(value(source.getQuantity()));
        }
        throw new DefinedException(new DefaultErrorCode("400", "请填写变更金额"));
    }

    public BigDecimal costDelta(ContractChangeType type, ContractLine source, BigDecimal requested,
                                BigDecimal newQuantity, BigDecimal newPrice, BigDecimal newCost) {
        if (requested != null) return requested;
        if (source == null || source.getQuantity() == null || source.getQuantity().signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal oldUnitCost = value(source.getCost()).divide(source.getQuantity(), 2, RoundingMode.HALF_UP);
        if (type == ContractChangeType.Quantity || type == ContractChangeType.Replace) {
            BigDecimal quantity = newQuantity == null ? source.getQuantity() : newQuantity;
            return quantity.subtract(value(source.getQuantity())).multiply(oldUnitCost);
        }
        if (type == ContractChangeType.Price) {
            BigDecimal cost = newCost == null ? oldUnitCost : newCost;
            return cost.subtract(oldUnitCost).multiply(value(source.getQuantity()));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal unitPrice(ContractLine source) {
        if (source == null || source.getQuantity() == null || source.getQuantity().signum() == 0) {
            return BigDecimal.ZERO;
        }
        return value(source.getAmount()).divide(source.getQuantity(), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 服务端计算结果；调用方不得自行改写 amountDelta/costDelta。 */
    public record Computed(BigDecimal oldQuantity, BigDecimal newQuantity, BigDecimal oldPrice, BigDecimal newPrice,
                           BigDecimal amountDelta, BigDecimal costDelta) {
    }
}
