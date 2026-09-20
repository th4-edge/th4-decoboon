package com.th4.edge.decoboon.customer.service.tag.page.support;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** 客户标签字典分页实现。 */
@Note("客户标签字典分页")
@Properties(allow = {
        @AllowProperties(model = CustomerTag.class,
                properties = {"tagId", "name", "category", "scope", "color", "status", "sort"})
})
@Service("customer/customer-tag/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.tag.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private TagCategory category;
    @AutoInject
    private TagStatus status;
    /** 标签名称关键词。 */
    @AutoInject
    private String keyword;

    private final TenantContext tenantContext;
    private final CustomerTagRepository tagRepository;

    public Page(TenantContext tenantContext, CustomerTagRepository tagRepository) {
        this.tenantContext = tenantContext;
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CustomerTag> call(CustomerTag request) {
        Employee currentEmployee = tenantContext.requireEmployee(employee);
        Long comId = currentEmployee.getCompany().getComId();
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<CustomerTag> all = tagRepository.findByCompany_ComIdOrderBySortAscTagIdAsc(comId).stream()
                .filter(item -> category == null || item.getCategory() == category)
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> normalized.isEmpty() || (item.getName() != null && item.getName().toLowerCase(Locale.ROOT).contains(normalized)))
                .toList();
        int pageIndex = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 50 : Math.min(size, 200);
        int from = Math.min(pageIndex * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return new PageImpl<>(all.subList(from, to), PageRequest.of(pageIndex, pageSize), all.size());
    }
}
