package com.th4.edge.decoboon.material.service.category.page.support;

import com.th4.edge.decoboon.material.model.MaterialCategory;
import com.th4.edge.decoboon.material.repository.MaterialCategoryRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/** 主材分类分页。 */
@Note("主材分类分页")
@Properties(allow = @AllowProperties(model = MaterialCategory.class,
        properties = {"materialCategoryId", "name", "defaultLossRate", "sort", "active", "remark", "createTime"}))
@Service("material/category/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.material.service.category.page.Page {

    @User private Employee employee;
    @AutoInject private Boolean activeOnly;

    private final TenantContext tenantContext;
    private final MaterialCategoryRepository repository;

    public Page(TenantContext tenantContext, MaterialCategoryRepository repository) {
        this.tenantContext = tenantContext;
        this.repository = repository;
    }

    @Override
    public List<MaterialCategory> call(MaterialCategory request) {
        Employee current = tenantContext.requireEmployee(employee);
        var rows = repository.findByCompany_ComIdOrderBySortAscMaterialCategoryIdAsc(current.getCompany().getComId());
        if (Boolean.TRUE.equals(activeOnly)) {
            rows = rows.stream().filter(item -> Boolean.TRUE.equals(item.getActive())).toList();
        }
        return rows;
    }
}
