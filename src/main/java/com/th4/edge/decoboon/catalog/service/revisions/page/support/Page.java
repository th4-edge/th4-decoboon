package com.th4.edge.decoboon.catalog.service.revisions.page.support;

import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.catalog.model.CatalogItemRevision;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRevisionRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 报价项目修订历史只读实现；成本快照仅对有成本权限的角色返回。 */
@Note("报价项目修订历史分页")
@Properties(allow = {
        @AllowProperties(model = CatalogItemRevision.class,
                properties = {"revisionId", "itemId", "itemCode", "revision", "changeType", "snapshotJson",
                        "snapshotHash", "changedBy", "changedAt", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("catalog/item/revision/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.catalog.service.revisions.page.Page {

    @AutoInject
    @NotNull
    private Long itemId;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @User
    private Employee employee;

    private final CatalogItemRevisionRepository repository;
    private final CatalogContext context;
    private final CatalogCostContext costContext;

    public Page(CatalogItemRevisionRepository repository, CatalogContext context, CatalogCostContext costContext) {
        this.repository = repository;
        this.context = context;
        this.costContext = costContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CatalogItemRevision> call(CatalogItemRevision request) {
        Employee currentEmployee = context.requireEmployee(employee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 30 : Math.min(size, 100);
        org.springframework.data.domain.Page<CatalogItemRevision> result = repository.findByCompany_ComIdAndItemId(
                currentEmployee.getCompany().getComId(), itemId,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "revision")));
        if (!costContext.canViewCost(currentEmployee)) {
            result.forEach(revision -> revision.setSnapshotJson(null));
        }
        return result;
    }
}
