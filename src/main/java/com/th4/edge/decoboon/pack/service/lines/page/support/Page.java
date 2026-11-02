package com.th4.edge.decoboon.pack.service.lines.page.support;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.context.PackageCostContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.service.annotation.ServiceName;
import com.th4.edge.framework.service.page.context.AbstractPage;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Scope;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Service;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;

/**
 * 套餐分项分页实现。
 *
 * <p>使用框架 Criteria 能力支持前端统一查询语法，并保留 BIM 主数据的分页读取能力。</p>
 */
@Note("套餐分项分页")
@Properties(allow = {
        @AllowProperties(model = PackageLine.class,
                properties = {"itemId", "template", "section", "item", "name", "brand", "spec", "unit",
                        "quantity", "quantityFactor", "pricingMethod", "included", "deductible", "sort",
                        "sectionNameSnapshot", "catalogTypeSnapshot", "catalogCategorySnapshot", "craftSnapshot",
                        "costPriceSnapshot", "criterions", "createTime"}),
        @AllowProperties(model = PackageTemplate.class, properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price"}),
        @AllowProperties(model = PackageSection.class, properties = {"groupId", "name"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "code", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "fixedTotalPrice", "formula", "craft"})
})
@Service("pack/line/page")
@ServiceName("page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page extends AbstractPage<PackageLine> implements com.th4.edge.decoboon.pack.service.lines.page.Page {

    @User
    private Employee employee;
    private final TenantContext tenantContext;
    private final PackageCostContext costContext;

    @Override
    public org.springframework.data.domain.Page<PackageLine> call(PackageLine request) {
        Employee current = tenantContext.requireEmployee(employee);
        request.setCompany(current.getCompany());
        return costContext.maskLines(super.call(request), current);
    }

    public Page(EntityManager entityManager, TenantContext tenantContext, PackageCostContext costContext) {
        super(entityManager);
        this.tenantContext = tenantContext;
        this.costContext = costContext;
    }
}
