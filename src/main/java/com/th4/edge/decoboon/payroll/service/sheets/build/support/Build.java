package com.th4.edge.decoboon.payroll.service.sheets.build.support;

import com.th4.edge.decoboon.payroll.context.PayrollBuildContext;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 生成工资单草稿。 */
@Note("工资单生成")
@Description("按薪酬方案和已确认考勤生成工资单草稿")
@Properties(allow = {
        @AllowProperties(model = PayrollSheet.class,
                properties = {"sheetId", "period", "status", "attendanceComplete", "commissionComplete", "totalGrossAmount",
                "totalDeductionAmount", "totalNetAmount", "confirmedBy", "confirmedAt", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("payroll/sheet/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.payroll.service.sheets.build.Build {

    @User
    private Employee employee;

    @AutoInject
    @NotEmpty
    private String period;

    private final PayrollBuildContext buildContext;

    public Build(PayrollBuildContext buildContext) {
        this.buildContext = buildContext;
    }

    @Override
    public PayrollSheet call(PayrollSheet request) {
        return buildContext.build(employee, period);
    }
}
