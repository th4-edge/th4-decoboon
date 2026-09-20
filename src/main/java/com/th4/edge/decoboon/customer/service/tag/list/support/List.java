package com.th4.edge.decoboon.customer.service.tag.list.support;

import com.th4.edge.decoboon.customer.configure.TagCategory;
import com.th4.edge.decoboon.customer.configure.TagStatus;
import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.decoboon.customer.repository.CustomerTagRepository;
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
import org.springframework.transaction.annotation.Transactional;

/** 可用标签字典列表实现。 */
@Note("可用标签字典列表")
@Properties(allow = {
        @AllowProperties(model = CustomerTag.class,
                properties = {"tagId", "name", "category", "scope", "color", "status", "sort"})
})
@Service("customer/customer-tag/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.customer.service.tag.list.List {

    @User
    private Employee employee;
    @AutoInject
    private TagCategory category;

    private final TenantContext tenantContext;
    private final CustomerTagRepository tagRepository;

    public List(TenantContext tenantContext, CustomerTagRepository tagRepository) {
        this.tenantContext = tenantContext;
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<CustomerTag> call(CustomerTag request) {
        Employee currentEmployee = tenantContext.requireEmployee(employee);
        Long comId = currentEmployee.getCompany().getComId();
        return category == null
                ? tagRepository.findByCompany_ComIdAndStatusOrderBySortAscTagIdAsc(comId, TagStatus.Enabled)
                : tagRepository.findByCompany_ComIdAndCategoryAndStatusOrderBySortAscTagIdAsc(comId, category, TagStatus.Enabled);
    }
}
