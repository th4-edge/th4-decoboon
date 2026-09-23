package com.th4.edge.decoboon.material.service.category.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.material.model.MaterialCategory;
import com.th4.edge.decoboon.material.repository.MaterialCategoryRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 维护企业主材分类与默认损耗率（DEC-MP-09）。
 *
 * <p>损耗率按百分比录入（如 8 表示 8%），默认 0；产品不预置行业比例，由企业自行设定。</p>
 */
@Note("主材分类保存")
@Properties(allow = @AllowProperties(model = MaterialCategory.class,
        properties = {"materialCategoryId", "name", "defaultLossRate", "sort", "active", "remark", "createTime"}))
@Service("material/category/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.material.service.category.save.Save {

    @User private Employee employee;
    @AutoInject @NotEmpty @Length(max = 64) private String name;
    @AutoInject private BigDecimal defaultLossRate;
    @AutoInject private Integer sort;
    @AutoInject private Boolean active;
    @AutoInject @Length(max = 500) private String remark;

    private final TenantContext tenantContext;
    private final MaterialCategoryRepository repository;
    private final ModelIdentityContext modelIdentityContext;

    public Save(TenantContext tenantContext, MaterialCategoryRepository repository,
                ModelIdentityContext modelIdentityContext) {
        this.tenantContext = tenantContext;
        this.repository = repository;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Override
    @Transactional
    public MaterialCategory call(MaterialCategory request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss,
                EmployeeRole.StoreManager, EmployeeRole.Finance);
        Long comId = current.getCompany().getComId();
        String normalized = name.trim();
        BigDecimal rate = defaultLossRate == null ? BigDecimal.ZERO : defaultLossRate;
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "损耗率需在 0～100 之间（百分比）"));
        }
        MaterialCategory target = repository
                .findFirstByCompany_ComIdAndNameIgnoreCase(comId, normalized)
                .orElseGet(MaterialCategory::new);
        target.setCompany(current.getCompany());
        target.setName(normalized);
        target.setDefaultLossRate(rate);
        target.setSort(sort == null ? 0 : sort);
        target.setActive(active == null ? Boolean.TRUE : active);
        target.setRemark(remark);
        target.setCreatedBy(current);
        return repository.save(modelIdentityContext.prepare(target));
    }
}
