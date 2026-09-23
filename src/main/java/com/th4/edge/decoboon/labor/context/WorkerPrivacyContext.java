package com.th4.edge.decoboon.labor.context;

import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 工人档案的权限与敏感字段裁剪。
 *
 * <p>维护角色：项目经理。查看角色：财务、企业负责人、店长、设计师、项目经理。
 * 身份证号与银行账号完整值仅企业负责人、财务与维护人（项目经理）可见，
 * 店长与设计师返回脱敏值；不依赖前端隐藏。</p>
 */
@Component
public class WorkerPrivacyContext {

    private static final Set<EmployeeRole> SENSITIVE_FULL = EnumSet.of(
            EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.Pm);

    private final TenantContext tenantContext;

    public WorkerPrivacyContext(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /** 维护（新增 / 编辑）：项目经理。 */
    public Employee requireMaintainer(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Pm);
    }

    /** 查看：财务、企业负责人、店长、设计师、项目经理。 */
    public Employee requireViewer(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.StoreManager,
                EmployeeRole.Pm, EmployeeRole.Finance, EmployeeRole.Designer);
    }

    /** 当前员工是否可见完整敏感字段。 */
    public boolean canViewSensitive(Employee employee) {
        return employee != null && employee.getRole() != null
                && SENSITIVE_FULL.contains(employee.getRole());
    }

    /** 无完整权限时以脱敏值替换身份证号与银行账号。 */
    public Worker mask(Worker worker, Employee employee) {
        if (worker == null || canViewSensitive(employee)) return worker;
        worker.setIdCardNo(maskIdCard(worker.getIdCardNo()));
        worker.setBankAccount(maskTail(worker.getBankAccount(), 4));
        return worker;
    }

    /** 身份证号脱敏：保留前 6 位与后 4 位。 */
    private String maskIdCard(String value) {
        if (value == null || value.isBlank()) return value;
        String digits = value.trim();
        if (digits.length() <= 10) return maskTail(digits, 0);
        return digits.substring(0, 6) + "********" + digits.substring(digits.length() - 4);
    }

    /** 账号脱敏：保留后 keep 位，其余以 * 替换。 */
    private String maskTail(String value, int keep) {
        if (value == null || value.isBlank()) return value;
        String digits = value.trim();
        if (digits.length() <= keep) return "*".repeat(digits.length());
        return "*".repeat(digits.length() - keep) + digits.substring(digits.length() - keep);
    }
}
