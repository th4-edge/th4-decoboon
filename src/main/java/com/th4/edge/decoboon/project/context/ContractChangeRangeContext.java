package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * 增减项当前有效剩余与待审批占用投影。
 *
 * <p>来源 S 可以是原合同行，也可以是此前已生效的新增/替换目标范围。
 * 本上下文只基于已存在的变更明细计算结果，不在合同行或变更明细上写回可变剩余量。</p>
 */
@Note("增减项当前有效剩余投影")
@Component
public class ContractChangeRangeContext {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ContractChangeItemRepository itemRepository;

    public ContractChangeRangeContext(ContractChangeItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /** 原合同行的当前有效剩余。 */
    public RangeSnapshot snapshot(ProjectContract contract, ContractLine sourceLine) {
        return snapshot(contract, sourceLine, null);
    }

    /** 原合同行的当前有效剩余；审批复核时可排除当前变更单自身。 */
    public RangeSnapshot snapshot(ProjectContract contract, ContractLine sourceLine, Long excludeChangeId) {
        if (contract == null || sourceLine == null) {
            throw new DefinedException(new DefaultErrorCode("400", "增减项来源不能为空"));
        }
        if (sourceLine.getContract() != null
                && contract.getContractId() != null
                && !Objects.equals(contract.getContractId(), sourceLine.getContract().getContractId())) {
            throw new DefinedException(new DefaultErrorCode("404", "原合同行不属于当前合同"));
        }
        return snapshot(contract, SourceKey.contractLine(sourceLine.getLineId()),
                value(sourceLine.getQuantity()), value(sourceLine.getAmount()), excludeChangeId);
    }

    /** 已生效新增/替换目标范围的当前有效剩余。 */
    public RangeSnapshot snapshot(ProjectContract contract, ContractChangeItem sourceChangeItem) {
        return snapshot(contract, sourceChangeItem, null);
    }

    /** 已生效新增/替换目标范围的当前有效剩余；审批复核时可排除当前变更单自身。 */
    public RangeSnapshot snapshot(ProjectContract contract, ContractChangeItem sourceChangeItem, Long excludeChangeId) {
        if (contract == null || sourceChangeItem == null) {
            throw new DefinedException(new DefaultErrorCode("400", "增减项来源不能为空"));
        }
        ContractChange change = sourceChangeItem.getChange();
        if (change != null && change.getContract() != null
                && contract.getContractId() != null
                && !Objects.equals(contract.getContractId(), change.getContract().getContractId())) {
            throw new DefinedException(new DefaultErrorCode("404", "变更目标来源不属于当前合同"));
        }
        if (change != null && change.getStatus() != ContractChangeStatus.CustomerConfirmed) {
            throw new DefinedException(new DefaultErrorCode("400", "变更目标尚未生效，不能作为减项或替换来源"));
        }
        BigDecimal quantity = firstNotNull(sourceChangeItem.getTargetQuantity(), sourceChangeItem.getNewQuantity());
        BigDecimal amount = firstNotNull(sourceChangeItem.getTargetAmount(), amountOf(sourceChangeItem));
        return snapshot(contract, SourceKey.changeItem(sourceChangeItem.getChangeItemId()), quantity, amount, excludeChangeId);
    }

    private RangeSnapshot snapshot(ProjectContract contract, SourceKey sourceKey,
                                   BigDecimal initialQuantity, BigDecimal initialAmount, Long excludeChangeId) {
        List<ContractChangeItem> items = itemRepository
                .findByChange_Contract_ContractIdOrderByChangeItemIdAsc(contract.getContractId());
        BigDecimal effectiveClosedQuantity = ZERO;
        BigDecimal effectiveClosedAmount = ZERO;
        BigDecimal pendingClosedQuantity = ZERO;
        String blockReason = null;

        for (ContractChangeItem item : items) {
            if (!matches(item, sourceKey)) {
                continue;
            }
            if (excludeChangeId != null && item.getChange() != null
                    && excludeChangeId.equals(item.getChange().getChangeId())) {
                continue;
            }
            ContractChangeStatus status = item.getChange() == null ? null : item.getChange().getStatus();
            if (status == null || status == ContractChangeStatus.Rejected || status == ContractChangeStatus.Cancelled) {
                continue;
            }
            if (status == ContractChangeStatus.Approved || status == ContractChangeStatus.Pending) {
                pendingClosedQuantity = pendingClosedQuantity.add(closedQuantity(item));
                continue;
            }
            if (status != ContractChangeStatus.CustomerConfirmed) {
                continue;
            }

            if (item.getChangeType() == ContractChangeType.Price) {
                // 新口径不再新建调价。历史调价若无法从快照稳定推导，则来源暂不可确认。
                if (item.getSourceRemainingAmount() == null || item.getSourceUnitPrice() == null) {
                    blockReason = "历史调价缺少可推导快照，当前有效剩余暂不可确认";
                }
                continue;
            }
            if (item.getChangeType() == ContractChangeType.Replace
                    && item.getSourceRemainingAmount() == null) {
                blockReason = "历史替换缺少来源剩余快照，当前有效剩余暂不可确认";
                continue;
            }
            effectiveClosedQuantity = effectiveClosedQuantity.add(closedQuantity(item));
            effectiveClosedAmount = effectiveClosedAmount.add(closedAmount(item, blockReason));
        }

        BigDecimal effectiveRemainingQuantity = maxZero(initialQuantity.subtract(effectiveClosedQuantity));
        BigDecimal effectiveRemainingAmount = maxZero(initialAmount.subtract(effectiveClosedAmount));
        BigDecimal availableQuantity = maxZero(effectiveRemainingQuantity.subtract(pendingClosedQuantity));
        BigDecimal currentUnitPrice = initialQuantity.signum() == 0
                ? ZERO
                : money(initialAmount.divide(initialQuantity, 4, RoundingMode.HALF_UP));
        boolean confirmable = blockReason == null;
        return new RangeSnapshot(initialQuantity, effectiveRemainingQuantity, pendingClosedQuantity,
                availableQuantity, effectiveRemainingAmount, currentUnitPrice, confirmable, blockReason);
    }

    private boolean matches(ContractChangeItem item, SourceKey sourceKey) {
        if (sourceKey.contractLineId() != null) {
            return item.getSourceLine() != null
                    && Objects.equals(sourceKey.contractLineId(), item.getSourceLine().getLineId());
        }
        return item.getSourceChangeItem() != null
                && Objects.equals(sourceKey.changeItemId(), item.getSourceChangeItem().getChangeItemId());
    }

    private BigDecimal closedQuantity(ContractChangeItem item) {
        if (item.getChangeType() == ContractChangeType.Add) {
            return ZERO;
        }
        BigDecimal oldQuantity = firstNotNull(item.getOldQuantity(), item.getSourceRemainingQuantity());
        if (item.getChangeType() == ContractChangeType.Quantity) {
            return maxZero(oldQuantity.subtract(value(item.getNewQuantity())));
        }
        return maxZero(oldQuantity);
    }

    private BigDecimal closedAmount(ContractChangeItem item, String blockReason) {
        if (item.getChangeType() == ContractChangeType.Replace
                && item.getSourceRemainingAmount() != null) {
            return item.getSourceRemainingAmount();
        }
        if (item.getSourceRemainingAmount() != null
                && item.getSourceRemainingQuantity() != null
                && item.getNewQuantity() != null
                && item.getChangeType() == ContractChangeType.Quantity) {
            BigDecimal remainingRatio = item.getSourceRemainingQuantity().signum() == 0
                    ? ZERO
                    : item.getNewQuantity().divide(item.getSourceRemainingQuantity(), 6, RoundingMode.HALF_UP);
            return maxZero(item.getSourceRemainingAmount().multiply(BigDecimal.ONE.subtract(remainingRatio)));
        }
        if (item.getAmountDelta() != null) {
            return item.getAmountDelta().abs();
        }
        if (item.getSourceRemainingAmount() != null) {
            return item.getSourceRemainingAmount();
        }
        blockReason = "历史变更缺少可推导金额快照，当前有效剩余暂不可确认";
        return ZERO;
    }

    private BigDecimal amountOf(ContractChangeItem item) {
        if (item.getNewPrice() == null || item.getNewQuantity() == null) {
            return ZERO;
        }
        return money(item.getNewPrice().multiply(item.getNewQuantity()));
    }

    private BigDecimal firstNotNull(BigDecimal first, BigDecimal second) {
        return first == null ? value(second) : first;
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value.signum() < 0 ? ZERO : value;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record SourceKey(Long contractLineId, Long changeItemId) {
        static SourceKey contractLine(Long id) {
            return new SourceKey(id, null);
        }

        static SourceKey changeItem(Long id) {
            return new SourceKey(null, id);
        }
    }

    /** 服务端权威范围投影；前端只能展示，不能据此直接提交可信金额。 */
    public record RangeSnapshot(
            BigDecimal initialQuantity,
            BigDecimal effectiveRemainingQuantity,
            BigDecimal pendingReservedQuantity,
            BigDecimal availableQuantity,
            BigDecimal effectiveRemainingAmount,
            BigDecimal currentUnitPrice,
            boolean confirmable,
            String blockReason) {
    }
}
