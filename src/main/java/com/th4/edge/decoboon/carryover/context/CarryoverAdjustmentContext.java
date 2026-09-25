package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverAdjustmentType;
import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverAdjustment;
import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverAdjustmentRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverCostSnapshotRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverProjectRepository;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 锁定后的承接基线调整。 */
@Note("承接基线调整")
@Component
public class CarryoverAdjustmentContext {

    private final CarryoverProjectRepository projectRepository;
    private final CarryoverAdjustmentRepository adjustmentRepository;
    private final CarryoverCostSnapshotRepository costRepository;
    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;

    public CarryoverAdjustmentContext(CarryoverProjectRepository projectRepository,
                                      CarryoverAdjustmentRepository adjustmentRepository,
                                      CarryoverCostSnapshotRepository costRepository,
                                      ReceivableRepository receivableRepository,
                                      PayableRepository payableRepository) {
        this.projectRepository = projectRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.costRepository = costRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
    }

    public CarryoverAdjustment adjust(Employee current, Long itemId, CarryoverAdjustmentType type,
                                      BigDecimal afterAmount, String reason) {
        if (type == null || afterAmount == null || afterAmount.signum() < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "调整类型和调整后金额不能为空，且金额不能为负"));
        }
        if (reason == null || reason.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "调整原因不能为空"));
        }
        CarryoverProject item = projectRepository.findLockedByItemId(itemId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问")));
        if (item.getCompany() == null || !item.getCompany().getComId().equals(current.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问"));
        }
        if (item.getStatus() != CarryoverProjectStatus.Locked) {
            throw new DefinedException(new DefaultErrorCode("400", "项目尚未锁定，请使用撤回修改流程"));
        }
        BigDecimal before = switch (type) {
            case Receivable -> item.getReceivableOutstanding();
            case Payable -> item.getPayableOutstanding();
            case Cost -> item.getCostTotal();
            case Other -> BigDecimal.ZERO;
        };
        BigDecimal beforeValue = value(before);
        BigDecimal delta = afterAmount.subtract(beforeValue);
        switch (type) {
            case Receivable -> {
                item.setReceivableOutstanding(afterAmount);
                syncReceivable(item, afterAmount);
            }
            case Payable -> {
                item.setPayableOutstanding(afterAmount);
                syncPayable(item, afterAmount);
            }
            case Cost -> {
                item.setCostTotal(afterAmount);
                syncCost(item, afterAmount);
            }
            case Other -> {
                // 非金额类更正只记录原因和版本，不回写金额。
            }
        }
        projectRepository.save(item);
        CarryoverAdjustment adjustment = new CarryoverAdjustment();
        adjustment.setCompany(current.getCompany());
        adjustment.setItem(item);
        adjustment.setType(type);
        adjustment.setBeforeAmount(before);
        adjustment.setDeltaAmount(delta);
        adjustment.setAfterAmount(afterAmount);
        adjustment.setReason(reason.trim());
        adjustment.setCreatedBy(current);
        return adjustmentRepository.save(adjustment);
    }

    private void syncReceivable(CarryoverProject item, BigDecimal amount) {
        Receivable receivable = item.getReceivable();
        if (receivable == null) return;
        receivable.setAmount(amount);
        receivableRepository.save(receivable);
    }

    private void syncPayable(CarryoverProject item, BigDecimal amount) {
        Payable payable = item.getPayable();
        if (payable == null) return;
        payable.setAmount(amount);
        payableRepository.save(payable);
    }

    private void syncCost(CarryoverProject item, BigDecimal amount) {
        CarryoverCostSnapshot snapshot = costRepository.findByItem_ItemId(item.getItemId()).orElse(null);
        if (snapshot == null) return;
        snapshot.setTotalCost(amount);
        costRepository.save(snapshot);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
