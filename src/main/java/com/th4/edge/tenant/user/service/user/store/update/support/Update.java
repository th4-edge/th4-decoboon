package com.th4.edge.tenant.user.service.user.store.update.support;

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

/** 门店更新别名路由（`AGENTS.md` 固定能力名 `update`），实现见 {@link StoreStatusContext}。 */
@Note("门店更新")
@Description("门店更新的规范能力名入口（等同 user/store/status）：仅启用/停用可改，仅 BOSS，审计留痕")
@Properties(allow = @AllowProperties(model = Store.class,
        properties = {"storeId", "name", "defaultStore", "color", "addr", "status", "sort", "createTime"}))
@Service("user/store/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.tenant.user.service.user.store.update.Update {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long storeId;

    @AutoInject
    @NotNull
    private StoreStatus status;

    private final StoreStatusContext storeStatusContext;

    public Update(StoreStatusContext storeStatusContext) {
        this.storeStatusContext = storeStatusContext;
    }

    @Override
    @Transactional
    public Store call(Store request) {
        return storeStatusContext.change(employee, storeId, status);
    }
}
