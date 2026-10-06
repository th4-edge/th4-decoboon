package com.th4.edge.decoboon.payroll.service.commissionadjustments.page.support;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.CommissionAdjustment;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.repository.CommissionAdjustmentRepository;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Note("提成负向调整分页")
@Properties(allow = {
        @AllowProperties(model = CommissionAdjustment.class,
                properties = {"adjustmentId", "employee", "project", "originalCommission", "refund", "payrollItem", "type", "status",
                        "refundAmount", "deductionAmount", "appliedAmount", "occurredAt", "remark", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = CommissionItem.class,
                properties = {"commissionId", "contractAmount", "commissionAmount", "releasedAmount", "paidAmount", "status"}),
        @AllowProperties(model = PayrollItem.class, properties = {"itemId"})
        ,
        @AllowProperties(model = ReceiptRefund.class,
                properties = {"refundId", "receipt", "amount", "account", "refundAt", "reason"}),
        @AllowProperties(model = Receipt.class,
                properties = {"receiptId", "amount", "receivedAt", "payerName", "remark"})
})
@Service("payroll/commission/adjustment/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.payroll.service.commissionadjustments.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;

    private final CommissionAdjustmentRepository repository;
    private final PayrollAccessContext accessContext;

    public Page(CommissionAdjustmentRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<CommissionAdjustment> call(CommissionAdjustment request) {
        Employee currentEmployee = accessContext.requireManager(employee);
        var items = repository.findAll(accessContext.visible(currentEmployee, CommissionAdjustment.class));
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
