package com.th4.edge.decoboon.customer.service.tag.delete.support;

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
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 停用标签字典项实现（软删）。 */
@Note("停用客户标签字典项")
@Properties(allow = {
        @AllowProperties(model = CustomerTag.class,
                properties = {"tagId", "name", "category", "scope", "color", "status", "sort"})
})
@Service("customer/customer-tag/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.customer.service.tag.delete.Delete {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long tagId;

    private final TenantContext tenantContext;
    private final CustomerTagRepository tagRepository;

    public Delete(TenantContext tenantContext, CustomerTagRepository tagRepository) {
        this.tenantContext = tenantContext;
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional
    public CustomerTag call(CustomerTag request) {
        Employee current = tenantContext.requireEmployee(employee);
        requireDictionaryMaintainer(current);
        Long comId = current.getCompany().getComId();
        CustomerTag tag = tagRepository.findByCompany_ComIdAndTagId(comId, tagId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "标签不存在或无权访问")));
        if (tag.getScope() == TagScope.System) {
            throw new DefinedException(new DefaultErrorCode("403", "系统标签不可删除或停用"));
        }
        tag.setStatus(TagStatus.Disabled);
        return tagRepository.save(tag);
    }

    /** 标签字典仅企业负责人、店长与财务可维护。 */
    private void requireDictionaryMaintainer(Employee current) {
        EmployeeRole role = current.getRole();
        if (role != EmployeeRole.Boss && role != EmployeeRole.StoreManager && role != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "仅企业负责人、店长或财务可维护标签字典"));
        }
    }
}
