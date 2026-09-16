package com.th4.edge.tenant.user.service.user.store.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Setting;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业负责人新增门店，服务端强校验套餐门店额度。
 */
@Note("新增门店")
@Description("企业负责人新增非默认门店，超过套餐上限时提示升级")
@Properties(allow = @AllowProperties(model = Store.class,
        properties = {"storeId", "name", "defaultStore", "color", "addr", "createTime"}))
@Service("user/store/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.tenant.user.service.user.store.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotEmpty
    @Length(max = 16)
    private String name;

    @AutoInject
    @Length(max = 12)
    private String color;

    @AutoInject
    @Length(max = 256)
    private String addr;

    private final TenantContext tenantContext;
    private final SubscriptionContext subscriptionContext;
    private final StoreRepository storeRepository;
    private final CompanyRepository companyRepository;
    private final ModelIdentityContext modelIdentityContext;

    public Save(TenantContext tenantContext,
                SubscriptionContext subscriptionContext,
                StoreRepository storeRepository,
                CompanyRepository companyRepository,
                ModelIdentityContext modelIdentityContext) {
        this.tenantContext = tenantContext;
        this.subscriptionContext = subscriptionContext;
        this.storeRepository = storeRepository;
        this.companyRepository = companyRepository;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Override
    @Transactional
    public Store call(Store store) {
        Employee boss = tenantContext.requireBoss(employee);
        Company company = boss.getCompany();
        subscriptionContext.requireWritable(company);
        subscriptionContext.lockCurrent(company);

        Long comId = company.getComId();
        long usedStores = storeRepository.countByCompany_ComId(comId);
        subscriptionContext.requireStoreCapacity(company, usedStores);

        store.setCompany(company);
        store.setName(name.trim());
        store.setColor(color);
        store.setAddr(addr);
        store.setDefaultStore(false);
        Store saved = storeRepository.save(modelIdentityContext.prepare(store));

        Setting setting = company.getSetting();
        if (setting == null) {
            setting = new Setting();
            company.setSetting(setting);
        }
        if (!Boolean.TRUE.equals(setting.getEnableStore())) {
            setting.setEnableStore(true);
            companyRepository.save(company);
        }
        return saved;
    }
}
