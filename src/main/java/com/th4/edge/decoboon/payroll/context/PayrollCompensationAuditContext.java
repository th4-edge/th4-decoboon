package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.tenant.user.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 薪酬方案结构化审计：事实形成前的编辑与删除必须可追溯，不新增数据库表。 */
@Component
public class PayrollCompensationAuditContext {

    private static final Logger LOG = LoggerFactory.getLogger(PayrollCompensationAuditContext.class);

    public void record(Employee operator, CompensationScheme scheme, String action, String beforeSnapshot) {
        if (operator == null || scheme == null) return;
        LOG.info("payroll_compensation_audit action={} operatorEmpId={} comId={} storeId={} schemeId={} employeeEmpId={} before={} after={}",
                action,
                operator.getEmpId(),
                operator.getCompany() == null ? null : operator.getCompany().getComId(),
                operator.getStore() == null ? null : operator.getStore().getStoreId(),
                scheme.getSchemeId(),
                scheme.getEmployee() == null ? null : scheme.getEmployee().getEmpId(),
                beforeSnapshot,
                snapshot(scheme));
    }

    public String snapshot(CompensationScheme scheme) {
        if (scheme == null) return "-";
        return "baseSalary=" + scheme.getBaseSalary()
                + ",scheduledDays=" + scheme.getScheduledDays()
                + ",effectiveFrom=" + scheme.getEffectiveFrom()
                + ",effectiveTo=" + scheme.getEffectiveTo()
                + ",status=" + scheme.getStatus()
                + ",remark=" + scheme.getRemark();
    }
}
