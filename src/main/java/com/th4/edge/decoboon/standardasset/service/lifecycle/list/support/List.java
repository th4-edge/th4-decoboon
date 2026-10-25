package com.th4.edge.decoboon.standardasset.service.lifecycle.list.support;

import com.th4.edge.decoboon.standardasset.context.StandardAssetLifecycleContext;
import com.th4.edge.decoboon.standardasset.model.StandardAssetLifecycleContract;
import com.th4.edge.decoboon.standardasset.model.StandardAssetStatusContract;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 企业运营标准资产生命周期只读契约实现。 */
@Note("企业运营标准资产生命周期列表")
@Properties(allow = {
        @AllowProperties(model = StandardAssetLifecycleContract.class,
                properties = {"assetType", "productName", "statuses", "versionMode", "sourceMode", "defaultPolicy",
                        "referenceFields", "snapshotFields", "historyPolicy"}),
        @AllowProperties(model = StandardAssetStatusContract.class,
                properties = {"key", "label", "editable", "newBusinessEligible", "historyReadable"})
})
@Service("standard-asset/lifecycle/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.standardasset.service.lifecycle.list.List {

    private final TenantContext tenantContext;
    private final StandardAssetLifecycleContext lifecycleContext;

    public List(TenantContext tenantContext, StandardAssetLifecycleContext lifecycleContext) {
        this.tenantContext = tenantContext;
        this.lifecycleContext = lifecycleContext;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<StandardAssetLifecycleContract> call(@User Employee employee) {
        tenantContext.requireEmployee(employee);
        return lifecycleContext.contracts();
    }
}
