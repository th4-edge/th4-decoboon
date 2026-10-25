package com.th4.edge.decoboon.payroll.service.sheets.info.support;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 工资单详情。 */
@Note("工资单详情")
@Properties(allow = {
        @AllowProperties(model = PayrollSheet.class,
                properties = {"sheetId", "period", "status", "attendanceComplete", "commissionComplete", "totalGrossAmount",
                "totalDeductionAmount", "totalNetAmount", "confirmedBy", "confirmedAt", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("payroll/sheet/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.payroll.service.sheets.info.Info {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long sheetId;

    private final PayrollSheetRepository repository;
    private final PayrollAccessContext accessContext;

    public Info(PayrollSheetRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollSheet call(PayrollSheet request) {
        Employee current = accessContext.requireManager(employee);
        Specification<PayrollSheet> byId = (root, query, cb) -> cb.equal(root.get("sheetId"), sheetId);
        return repository.findOne(byId.and(accessContext.visibleSheets(current)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工资单不存在或无权访问")));
    }
}
