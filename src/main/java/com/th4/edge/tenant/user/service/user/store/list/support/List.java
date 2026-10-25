package com.th4.edge.tenant.user.service.user.store.list.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.tenant.user.configure.DataScopeType;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 当前账号可见门店列表。
 *
 * <p>数据范围为企业的账号（BOSS、总部财务）可见企业全部门店；门店/团队/个人范围仅可见本人门店。
 * 前端据此判断是否需要展示"门店维度"筛选（仅一个门店时无需展示）。</p>
 *
 * <p>`P0D-04`：企业负责人视角**包含已停用门店**（历史查看需要），并返回 `status` / `sort`；
 * 排序按 `sort` 升序、同值按 `storeId` 升序。前端对 `status=Disabled` 的选项展示「已停用」标记，
 * 并禁止把停用门店作为**新建归属**（服务端另有 {@code StoreAttributionContext} 兜底拦截）。</p>
 */
@Note("门店列表")
@Properties(allow = {
        @AllowProperties(model = Store.class,
                properties = {"storeId", "name", "defaultStore", "addr", "status", "sort"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name"})
})
@Service("user/store/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.tenant.user.service.user.store.list.List {

    @User
    private Employee employee;

    /** 是否仅返回本人门店；不传时由角色决定（企业负责人返回全部）。 */
    @AutoInject
    private Boolean currentOnly;

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final StoreRepository storeRepository;

    public List(TenantContext tenantContext, DataScopeContext dataScopeContext, StoreRepository storeRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.storeRepository = storeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Store> call(Employee request) {
        Employee current = tenantContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        if (Boolean.TRUE.equals(currentOnly) || scope.type() != DataScopeType.Company) {
            return current.getStore() == null ? java.util.List.of() : java.util.List.of(current.getStore());
        }
        return scope.comId() == null ? java.util.List.of()
                : storeRepository.findByCompany_ComIdOrderBySortAscStoreIdAsc(scope.comId());
    }
}
