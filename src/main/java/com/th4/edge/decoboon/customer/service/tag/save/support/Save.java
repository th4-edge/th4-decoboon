package com.th4.edge.decoboon.customer.service.tag.save.support;

import com.th4.edge.decoboon.customer.configure.TagCategory;
import com.th4.edge.decoboon.customer.configure.TagScope;
import com.th4.edge.decoboon.customer.configure.TagStatus;
import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.decoboon.customer.repository.CustomerTagRepository;
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

/** 新建标签字典项实现。 */
@Note("新建客户标签字典项")
@Properties(allow = {
        @AllowProperties(model = CustomerTag.class,
                properties = {"tagId", "name", "category", "scope", "color", "status", "sort"})
})
@Service("customer/customer-tag/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.customer.service.tag.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotEmpty
    @Length(max = 20)
    private String name;

    @AutoInject
    private TagCategory category;

    @AutoInject
    @Length(max = 16)
    private String color;

    @AutoInject
    private Integer sort;

    private final TenantContext tenantContext;
    private final CustomerTagRepository tagRepository;

    public Save(TenantContext tenantContext, CustomerTagRepository tagRepository) {
        this.tenantContext = tenantContext;
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional
    public CustomerTag call(CustomerTag request) {
        Employee current = tenantContext.requireEmployee(employee);
        requireDictionaryMaintainer(current);
        Long comId = current.getCompany().getComId();
        String tagName = name == null ? "" : name.trim();
        if (tagName.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "标签名称不能为空"));
        }
        tagRepository.findByCompany_ComIdAndName(comId, tagName).ifPresent(existing -> {
            throw new DefinedException(new DefaultErrorCode("409", "标签名称已存在"));
        });
        CustomerTag tag = new CustomerTag();
        tag.setCompany(current.getCompany());
        tag.setName(tagName);
        tag.setCategory(category == null ? TagCategory.Intent : category);
        tag.setScope(TagScope.Manual);
        tag.setColor(color);
        tag.setStatus(TagStatus.Enabled);
        tag.setSort(sort == null ? 0 : sort);
        return tagRepository.save(tag);
    }

    /** 标签字典仅企业负责人与店长可维护。 */
    private void requireDictionaryMaintainer(Employee current) {
        EmployeeRole role = current.getRole();
        if (role != EmployeeRole.Boss && role != EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "仅企业负责人或店长可维护标签字典"));
        }
    }
}
