package com.th4.edge.decoboon.pack.service.templates.info.support;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.pack.context.PackageCostContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.model.PackageTemplateItem;
import com.th4.edge.decoboon.pack.model.PackageRule;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import org.springframework.context.annotation.Scope;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 装修套餐详情 Provider。
 */
@Note("套餐详情")
@Properties(allow = {
        @AllowProperties(model = PackageTemplate.class,
                properties = {"templateId", "code", "version", "status", "currentPublished", "publishedAt", "snapshotHash", "name", "area", "price", "discount", "cost", "items", "measures",
                        "criterions", "createTime", "updateTime"}),
        @AllowProperties(model = PackageLine.class,
                properties = {"itemId", "section", "sectionNameSnapshot", "item", "catalogTypeSnapshot",
                        "catalogCategorySnapshot", "craftSnapshot", "costPriceSnapshot", "name", "brand", "spec", "unit",
                        "quantity", "quantityFactor", "pricingMethod", "included", "deductible", "sort", "criterions"}),
        @AllowProperties(model = PackageSection.class,
                properties = {"groupId", "name", "spaceTypes", "maxDeleteNum", "priority"}),
        @AllowProperties(model = PackageTemplateItem.class,
                properties = {"itemId", "name", "unit", "spaceTypes", "priority"}),
        @AllowProperties(model = PackageRule.class, properties = {"price", "standard", "remark"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "costPrice", "formula", "craft", "criterion"})
})
@Service("pack/template/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.pack.service.templates.info.Info {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long templateId;

    private final PackageTemplateRepository giftPackRepository;
    private final TenantContext tenantContext;
    private final PackageCostContext costContext;

    public Info(PackageTemplateRepository giftPackRepository, TenantContext tenantContext,
                PackageCostContext costContext) {
        this.giftPackRepository = giftPackRepository;
        this.tenantContext = tenantContext;
        this.costContext = costContext;
    }

    @Override
    @Transactional(readOnly = true)
    public PackageTemplate call(PackageTemplate request) {
        Employee current = tenantContext.requireEmployee(employee);
        PackageTemplate template = giftPackRepository.findByTemplateIdAndCompany_ComId(
                        templateId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "套餐不存在或无权访问")));
        return costContext.mask(template, current);
    }
}
