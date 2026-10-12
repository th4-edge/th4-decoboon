package com.th4.edge.tenant.user.service.user.store.status.support;

import com.th4.edge.decoboon.common.context.StoreStatusContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.StoreStatus;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 门店启用/停用（`P0D-04`）。
 *
 * <p>路由为业务规格指定的独立动作 `user/store/status`（`DESIGN-BIZ-20261013-002` §3）；
 * 规则实现统一在 {@link StoreStatusContext}，与规范能力名别名 `user/store/update` 共用同一实现，
 * 不产生两套口径。</p>
 */
@Note("门店启用停用")
@Description("企业负责人停用或启用门店：默认总店不可停用、最后一家启用门店不可停用、仍在职人员或在手业务时阻断（GAP-051），动作写审计留痕")
@Properties(allow = @AllowProperties(model = Store.class,
        properties = {"storeId", "name", "defaultStore", "color", "addr", "status", "sort", "createTime"}))
@Service("user/store/status")
@Scope(ScopeTypes.PROTOTYPE)
public class Status implements com.th4.edge.tenant.user.service.user.store.status.Status {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long storeId;

    @AutoInject
    @NotNull
    private StoreStatus status;

    private final StoreStatusContext storeStatusContext;

    public Status(StoreStatusContext storeStatusContext) {
        this.storeStatusContext = storeStatusContext;
    }

    @Override
    @Transactional
    public Store call(Store request) {
        return storeStatusContext.change(employee, storeId, status);
    }
}
