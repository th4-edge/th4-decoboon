package com.th4.edge.decoboon.payroll.service.commissions.release.support;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.CommissionRelease;
import com.th4.edge.decoboon.payroll.repository.CommissionReleaseRepository;
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

@Note("提成释放记录")
@Properties(allow = {
        @AllowProperties(model = CommissionRelease.class,
                properties = {"releaseId", "employee", "commissionItem", "receipt", "amount", "releasedAt", "remark", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = CommissionItem.class, properties = {"commissionId"}),
        @AllowProperties(model = Receipt.class,
                properties = {"receiptId", "receivable", "amount", "receivedAt", "payerName", "remark"}),
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "name", "type", "amount", "receivedAmount", "status"})
})
@Service("payroll/commission/release/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Release implements com.th4.edge.decoboon.payroll.service.commissions.release.Release {
    @User private Employee employee;
    @AutoInject private Long commissionId;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final CommissionReleaseRepository repository;
    private final PayrollAccessContext accessContext;
    public Release(CommissionReleaseRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository; this.accessContext = accessContext;
    }
    @Override
    public org.springframework.data.domain.Page<CommissionRelease> call(CommissionRelease request) {
        Employee currentEmployee = accessContext.requireManager(employee);
        var items = repository.findAll(accessContext.visible(currentEmployee, CommissionRelease.class))
                .stream()
                .filter(item -> commissionId == null || commissionId.equals(item.getCommissionItem().getCommissionId()))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
