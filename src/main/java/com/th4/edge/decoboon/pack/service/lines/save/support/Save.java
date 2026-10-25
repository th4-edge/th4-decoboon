package com.th4.edge.decoboon.pack.service.lines.save.support;

import com.th4.edge.decoboon.pack.context.PackageConfigContext;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 套餐项目行保存实现。 */
@Note("套餐项目行保存")
@Properties(allow = {
        @AllowProperties(model = PackageLine.class,
                properties = {"itemId", "template", "section", "item", "name", "brand", "spec", "unit",
                        "quantity", "quantityFactor", "pricingMethod", "included", "deductible", "sort",
                        "sectionNameSnapshot", "catalogTypeSnapshot", "catalogCategorySnapshot", "craftSnapshot",
                        "costPriceSnapshot", "criterions"}),
        @AllowProperties(model = PackageTemplate.class, properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price", "discount", "cost"}),
        @AllowProperties(model = PackageSection.class, properties = {"groupId", "name"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "costPrice"})
})
@Service("pack/line/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.pack.service.lines.save.Save {

    @AutoInject
    private Long itemId;

    @AutoInject
    @NotNull
    private Long templateId;

    @AutoInject
    @NotNull
    private Long sectionId;

    @AutoInject
    @NotNull
    private Long catalogItemId;

    @User
    private Employee employee;

    private final PackageConfigContext configContext;

    public Save(PackageConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    public PackageLine call(@ModelSaveInject PackageLine request) {
        request.setItemId(itemId);
        return configContext.saveLine(request, templateId, sectionId, catalogItemId, employee);
    }
}
