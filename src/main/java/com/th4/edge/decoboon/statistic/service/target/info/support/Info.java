package com.th4.edge.decoboon.statistic.service.target.info.support;

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

import java.time.LocalDate;

/** 月度经营目标详情：店长锁定本店；BOSS 可按 storeId 读取任一门店或公司目标。 */
@Note("月度经营目标详情")
@Properties(allow = {
        @AllowProperties(model = BusinessTarget.class,
                properties = {"targetId", "targetMonth", "signedAmount", "receivedAmount", "grossProfit",
                        "activeProjectCount", "createTime", "updateTime", "store"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("statistic/target/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.statistic.service.target.info.Info {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private LocalDate month;

    @AutoInject
    private Long storeId;

    private final TenantContext tenantContext;
    private final BusinessTargetRepository targetRepository;
    private final StoreRepository storeRepository;

    public Info(TenantContext tenantContext, BusinessTargetRepository targetRepository,
                StoreRepository storeRepository) {
        this.tenantContext = tenantContext;
        this.targetRepository = targetRepository;
        this.storeRepository = storeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessTarget call(BusinessTarget request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.StoreManager);
        Company company = current.getCompany();
        if (company == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未归属企业"));
        }
        LocalDate targetMonth = month.withDayOfMonth(1);
        Store store = resolveStore(current, company);
        BusinessTarget target = (store == null
                ? targetRepository.findByCompany_ComIdAndStoreIsNullAndTargetMonth(company.getComId(), targetMonth)
                : targetRepository.findByCompany_ComIdAndStore_StoreIdAndTargetMonth(
                        company.getComId(), store.getStoreId(), targetMonth))
                .orElseGet(() -> empty(company, store, targetMonth));
        return target;
    }

    private BusinessTarget empty(Company company, Store store, LocalDate month) {
        BusinessTarget target = new BusinessTarget();
        target.setCompany(company);
        target.setStore(store);
        target.setTargetMonth(month);
        return target;
    }

    private Store resolveStore(Employee current, Company company) {
        if (current.getRole() == EmployeeRole.StoreManager) {
            Store own = current.getStore();
            if (own == null || own.getStoreId() == null) {
                throw new DefinedException(new DefaultErrorCode("400", "店长未归属门店"));
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
        return store;
    }
}
