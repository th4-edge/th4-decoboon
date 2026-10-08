package com.th4.edge.decoboon.designtemplate.context;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

/**
 * 设计模版权限上下文（BR-040 §6 / DEC-050）。
 *
 * <p>企业级与门店级模版由企业负责人、店长维护与发布；个人级模版由本人维护。
 * 个人级仅本人使用，不做点对点分享；需要分享改用门店级或企业级。</p>
 */
@Note("设计模版权限")
@Component
public class DesignTemplateAccessContext {

    private final TenantContext tenantContext;

    public DesignTemplateAccessContext(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /** 当前登录员工；企业/门店/角色一律从登录员工加载，不信任前端传值。 */
    public Employee requireEmployee(Employee employee) {
        Employee current = tenantContext.requireEmployee(employee);
        if (current == null) {
            throw denied("当前账号不可用");
        }
        return current;
    }

    public boolean isManager(Employee employee) {
        return employee.getRole() == EmployeeRole.Boss || employee.getRole() == EmployeeRole.StoreManager;
    }

    /** 创建指定归属范围的模版需要对应权限。 */
    public void requireCreate(Employee employee, DesignTemplate.Scope scope) {
        if (scope == DesignTemplate.Scope.Personal) {
            return;
        }
        if (!isManager(employee)) {
            throw denied("企业级/门店级模版仅企业负责人、店长可维护；设计师请建个人级模版");
        }
    }

    /** 维护（编辑/发布/停用）已存在的模版。 */
    public void requireMaintain(Employee employee, DesignTemplate template) {
        if (!sameCompany(employee, template)) {
            throw denied("模版不属于当前企业");
        }
        switch (template.getScope()) {
            case Company, Store -> {
                if (!isManager(employee)) {
                    throw denied("企业级/门店级模版仅企业负责人、店长可维护");
                }
                if (template.getScope() == DesignTemplate.Scope.Store
                        && !sameStore(employee, template)) {
                    throw denied("模版不属于当前门店");
                }
            }
            case Personal -> {
                if (!isOwner(employee, template) && !isManager(employee)) {
                    throw denied("个人级模版仅归属人可维护");
                }
            }
        }
    }

    /** 可见性：本人个人模版 + 本门店门店模版 + 本企业企业模版。 */
    public boolean visible(Employee employee, DesignTemplate template) {
        if (!sameCompany(employee, template)) {
            return false;
        }
        return switch (template.getScope()) {
            case Company -> true;
            case Store -> sameStore(employee, template);
            case Personal -> isOwner(employee, template) || isManager(employee);
        };
    }

    private boolean sameCompany(Employee employee, DesignTemplate template) {
        return template.getCompany() != null && employee.getCompany() != null
                && template.getCompany().getComId().equals(employee.getCompany().getComId());
    }

    private boolean sameStore(Employee employee, DesignTemplate template) {
        return template.getStore() != null && employee.getStore() != null
                && template.getStore().getStoreId().equals(employee.getStore().getStoreId());
    }

    private boolean isOwner(Employee employee, DesignTemplate template) {
        return template.getOwner() != null && template.getOwner().getEmpId().equals(employee.getEmpId());
    }

    private static DefinedException denied(String message) {
        return new DefinedException(new DefaultErrorCode("403", message));
    }
}
