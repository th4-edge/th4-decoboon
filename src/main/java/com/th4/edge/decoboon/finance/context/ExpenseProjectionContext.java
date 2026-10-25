package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 按当前用户与费用状态显式生成可执行动作和下一步。 */
@Component
public class ExpenseProjectionContext {

    private final ExpensePaymentContext paymentContext;

    public ExpenseProjectionContext(ExpensePaymentContext paymentContext) {
        this.paymentContext = paymentContext;
    }

    public Expense apply(Expense expense, Employee current) {
        List<String> actions = new ArrayList<>();
        if (expense == null || current == null) return expense;
        paymentContext.applyProjection(expense);
        boolean owner = same(expense.getOwner(), current);
        boolean requester = same(expense.getRequester(), current);
        boolean manager = current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.StoreManager;
        boolean finance = current.getRole() == EmployeeRole.Finance;

        switch (expense.getStatus()) {
            case Draft -> {
                if (owner || manager) actions.addAll(List.of("edit", "delete", "submit"));
            }
            case Returned -> {
                if (owner || manager) actions.addAll(List.of("edit", "submit"));
            }
            case Submitted -> {
                if (requester) actions.add("withdraw");
                if (manager && !owner) {
                    actions.addAll(List.of("approve", "return", "reject"));
                } else if (current.getRole() == EmployeeRole.Boss && owner) {
                    actions.add("approve");
                }
                if (finance && !owner) actions.add("return");
            }
            case Approved, Closed -> {
                if (paymentContext.canPay(expense, current)) actions.add("pay");
            }
            default -> { }
        }
        expense.setAllowedActions(actions);
        expense.setNextAction(nextAction(expense.getStatus(), actions));
        return expense;
    }

    private String nextAction(ExpenseStatus status, List<String> actions) {
        if (status == null) return "暂不可确认";
        return switch (status) {
            case Draft -> actions.contains("submit") ? "提交审批" : "等待费用归属人处理";
            case Returned -> actions.contains("submit") ? "补充后重新提交" : "等待费用归属人处理";
            case Submitted -> actions.contains("approve") ? "审批费用"
                    : actions.contains("return") ? "票据复核"
                    : actions.contains("withdraw") ? "等待审批" : "等待处理";
            case Approved -> actions.contains("pay") ? "安排付款" : "等待财务付款";
            case Rejected -> "已拒绝";
            case Cancelled -> "已取消";
            case Voided -> "已作废";
            case Closed -> "已完成";
        };
    }

    private boolean same(Employee employee, Employee current) {
        return employee != null && current != null
                && Objects.equals(employee.getEmpId(), current.getEmpId());
    }
}
