package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverPaymentDirection;
import com.th4.edge.decoboon.carryover.model.CarryoverPaymentPlan;
import com.th4.edge.decoboon.carryover.model.CarryoverPaymentPlanReport;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverPaymentPlanRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 剩余收付款计划：期初应收/应付的时间分解，不是新增应收应付。 */
@Component
public class CarryoverPaymentPlanContext {

    private final CarryoverAccessContext accessContext;
    private final CarryoverPaymentPlanRepository repository;

    public CarryoverPaymentPlanContext(CarryoverAccessContext accessContext,
                                       CarryoverPaymentPlanRepository repository) {
        this.accessContext = accessContext;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CarryoverPaymentPlanReport report(Employee principal, Long itemId) {
        CarryoverProject item = accessContext.requireItem(principal, itemId);
        return report(item);
    }

    /** 整批替换：一次提交该项目的全部计划行；合计不得超过期初余额。 */
    @Transactional
    public CarryoverPaymentPlanReport save(Employee principal, Long itemId, List<CarryoverPaymentPlan> plans) {
        Employee current = principal;
        CarryoverProject item = accessContext.requireFinanceEditableItem(current, itemId);
        List<CarryoverPaymentPlan> submitted = plans == null ? List.of() : plans;
        BigDecimal plannedReceivable = BigDecimal.ZERO;
        BigDecimal plannedPayable = BigDecimal.ZERO;
        for (CarryoverPaymentPlan plan : submitted) {
            if (plan == null) continue;
            if (plan.getDirection() == null) {
                throw new DefinedException(new DefaultErrorCode("400", "计划方向不能为空"));
            }
            BigDecimal amount = plan.getAmount() == null ? BigDecimal.ZERO : plan.getAmount();
            if (amount.signum() <= 0) {
                throw new DefinedException(new DefaultErrorCode("400", "计划金额必须大于 0"));
            }
            if (plan.getDirection() == CarryoverPaymentDirection.Receivable) {
                plannedReceivable = plannedReceivable.add(amount);
            } else {
                plannedPayable = plannedPayable.add(amount);
            }
        }
        if (plannedReceivable.compareTo(value(item.getReceivableOutstanding())) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "收款计划合计不能超过期初应收"));
        }
        if (plannedPayable.compareTo(value(item.getPayableOutstanding())) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款计划合计不能超过期初应付"));
        }

        repository.deleteByCompany_ComIdAndItem_ItemId(current.getCompany().getComId(), itemId);
        List<CarryoverPaymentPlan> saved = new ArrayList<>();
        int sort = 10;
        for (CarryoverPaymentPlan plan : submitted) {
            if (plan == null) continue;
            CarryoverPaymentPlan target = new CarryoverPaymentPlan();
            target.setCompany(current.getCompany());
            target.setItem(item);
            target.setDirection(plan.getDirection());
            target.setAmount(plan.getAmount());
            target.setPlannedDate(plan.getPlannedDate());
            target.setCounterparty(trim(plan.getCounterparty()));
            target.setRemark(trim(plan.getRemark()));
            target.setSort(sort);
            target.setCreatedBy(current);
            saved.add(repository.save(target));
            sort += 10;
        }
        return report(item, saved);
    }

    private CarryoverPaymentPlanReport report(CarryoverProject item) {
        List<CarryoverPaymentPlan> plans = repository
                .findByCompany_ComIdAndItem_ItemIdOrderByDirectionAscSortAscPlanIdAsc(
                        item.getCompany().getComId(), item.getItemId());
        return report(item, plans);
    }

    private CarryoverPaymentPlanReport report(CarryoverProject item, List<CarryoverPaymentPlan> plans) {
        BigDecimal plannedReceivable = BigDecimal.ZERO;
        BigDecimal plannedPayable = BigDecimal.ZERO;
        for (CarryoverPaymentPlan plan : plans) {
            BigDecimal amount = value(plan.getAmount());
            if (plan.getDirection() == CarryoverPaymentDirection.Receivable) {
                plannedReceivable = plannedReceivable.add(amount);
            } else {
                plannedPayable = plannedPayable.add(amount);
            }
        }
        CarryoverPaymentPlanReport report = new CarryoverPaymentPlanReport();
        report.setItemId(item.getItemId());
        report.setReceivableOutstanding(value(item.getReceivableOutstanding()));
        report.setPayableOutstanding(value(item.getPayableOutstanding()));
        report.setPlannedReceivable(plannedReceivable);
        report.setPlannedPayable(plannedPayable);
        report.setUnplannedReceivable(value(item.getReceivableOutstanding()).subtract(plannedReceivable));
        report.setUnplannedPayable(value(item.getPayableOutstanding()).subtract(plannedPayable));
        report.setPlans(plans);
        return report;
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
