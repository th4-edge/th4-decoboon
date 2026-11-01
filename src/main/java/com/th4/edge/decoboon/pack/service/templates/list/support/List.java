package com.th4.edge.decoboon.pack.service.templates.list.support;

import com.th4.edge.decoboon.pack.configure.PackageTemplateStatus;
import com.th4.edge.decoboon.pack.context.PackageCostContext;
import com.th4.edge.decoboon.pack.context.PackageSummaryContext;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 可用于新报价的当前发布套餐实现。 */
@Note("可用套餐模板")
@Properties(allow = @AllowProperties(model = PackageTemplate.class,
        properties = {"templateId", "code", "version", "status", "currentPublished", "name", "area", "price",
                "discount", "cost", "sectionCount", "lineCount", "ready", "criterions", "createTime", "updateTime"}))
@Service("pack/template/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.pack.service.templates.list.List {

    @User
    private Employee employee;

    private final PackageTemplateRepository templateRepository;
    private final TenantContext tenantContext;
    private final PackageCostContext costContext;
    private final PackageSummaryContext summaryContext;

    public List(PackageTemplateRepository templateRepository, TenantContext tenantContext,
                PackageCostContext costContext, PackageSummaryContext summaryContext) {
        this.templateRepository = templateRepository;
        this.tenantContext = tenantContext;
        this.costContext = costContext;
        this.summaryContext = summaryContext;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<PackageTemplate> call(@User Employee employee) {
        Employee current = tenantContext.requireEmployee(employee);
        var templates = templateRepository.findByCompany_ComIdAndStatusAndCurrentPublishedTrueOrderByCodeAsc(
                current.getCompany().getComId(), PackageTemplateStatus.Published);
        summaryContext.apply(templates, current.getCompany().getComId());
        return templates.stream().map(item -> costContext.mask(item, current)).toList();
    }
}
