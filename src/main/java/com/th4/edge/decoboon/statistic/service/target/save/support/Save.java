package com.th4.edge.decoboon.statistic.service.target.save.support;

import com.th4.edge.decoboon.statistic.model.BusinessTarget;
import com.th4.edge.decoboon.statistic.repository.BusinessTargetRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 月度经营目标保存（DEC-076 / BR-049）。
 *
 * <p>公司目标与门店目标分层：BOSS 可维护公司目标和任一门店目标；店长只能维护本人门店目标。
 * 两层目标不自动相加、不互相覆盖。</p>
 */
@Note("月度经营目标保存")
@Properties(allow = {
        @AllowProperties(model = BusinessTarget.class,
                properties = {"targetId", "targetMonth", "signedAmount", "receivedAmount", "grossProfit",
                        "activeProjectCount", "createTime", "updateTime", "store"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("statistic/target/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.statistic.service.target.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private LocalDate month;
    @AutoInject
    private BigDecimal signedAmount;
    @AutoInject
    private BigDecimal receivedAmount;
    @AutoInject
    private BigDecimal grossProfit;
    @AutoInject
    private Integer activeProjectCount;
    /** 为空/0 表示公司目标；BOSS 可指定门店；店长由服务端强制本店。 */
    @AutoInject
    private Long storeId;

    private final TenantContext tenantContext;
    private final BusinessTargetRepository targetRepository;
    private final StoreRepository storeRepository;

    public Save(TenantContext tenantContext, BusinessTargetRepository targetRepository,
                StoreRepository storeRepository) {
        this.tenantContext = tenantContext;
        this.targetRepository = targetRepository;
        this.storeRepository = storeRepository;
    }

    @Override
    @Transactional
    public BusinessTarget call(BusinessTarget request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.StoreManager);
        Company company = current.getCompany();
        if (company == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未归属企业"));
        }
        LocalDate targetMonth = month.withDayOfMonth(1);
        requireNonNegative(signedAmount, "签约目标");
        requireNonNegative(receivedAmount, "回款目标");
        requireNonNegative(grossProfit, "毛利目标");
        if (activeProjectCount != null && activeProjectCount < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "在建项目目标数不能小于 0"));
        }

        Store store = resolveStore(current, company);
        BusinessTarget target = (store == null
                ? targetRepository.findByCompany_ComIdAndStoreIsNullAndTargetMonth(company.getComId(), targetMonth)
                : targetRepository.findByCompany_ComIdAndStore_StoreIdAndTargetMonth(
                        company.getComId(), store.getStoreId(), targetMonth))
                .orElseGet(BusinessTarget::new);
        target.setCompany(company);
        target.setStore(store);
        target.setTargetMonth(targetMonth);
        target.setSignedAmount(value(signedAmount));
        target.setReceivedAmount(value(receivedAmount));
        target.setGrossProfit(value(grossProfit));
        target.setActiveProjectCount(activeProjectCount == null ? 0 : activeProjectCount);
        return targetRepository.save(target);
    }

    private Store resolveStore(Employee current, Company company) {
        if (current.getRole() == EmployeeRole.StoreManager) {
            Store own = current.getStore();
            if (own == null || own.getStoreId() == null) {
                throw new DefinedException(new DefaultErrorCode("400", "店长未归属门店，不能维护门店目标"));
            }
            if (!own.attributable()) {
                throw new DefinedException(new DefaultErrorCode("400", "门店已停用，不能维护门店目标"));
            }
            return own;
        }
        if (storeId == null || storeId <= 0) {
            return null;
        }
        Store store = storeRepository.findByStoreId(storeId);
        if (store == null || store.getCompany() == null
                || !store.getCompany().getComId().equals(company.getComId())) {
            throw new DefinedException(new DefaultErrorCode("404", "门店不存在或无权访问"));
        }
        if (!store.attributable()) {
            throw new DefinedException(new DefaultErrorCode("400", "门店已停用，不能维护门店目标"));
        }
        return store;
    }

    private void requireNonNegative(BigDecimal value, String label) {
        if (value != null && value.signum() < 0) {
            throw new DefinedException(new DefaultErrorCode("400", label + "不能小于 0"));
        }
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
