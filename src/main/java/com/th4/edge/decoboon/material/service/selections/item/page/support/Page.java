package com.th4.edge.decoboon.material.service.selections.item.page.support;

import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.model.MaterialSelectionItem;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 以合同主材行为清单来源，返回项目逐项选材与下单状态。 */
@Note("项目主材分项清单")
@Description("返回项目全部合同主材分项、最新选材结果版本及材料计划派生的下单状态")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.decoboon.material.model.MaterialSelectionItem.class,
                properties = {"itemId", "projectId", "projectNo", "projectName", "propertyId", "propertyAddress", "customerName", "designerName", "contractId", "contractNo", "spaceName", "category", "materialName", "brand", "spec", "unit", "quantity", "sort", "required", "selectionId", "version", "status", "statusName", "selectionText", "selectionJson", "attachments", "updatedAt", "orderStatus", "orderStatusName", "nextAction"})
})
@Service("material/selection/item/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.material.service.selections.item.page.Page {

    @AutoInject @NotNull private Long projectId;
    @User private Employee employee;
    private final MaterialSelectionContext selectionContext;
    private final ProjectAccessContext projectAccessContext;

    public Page(MaterialSelectionContext selectionContext, ProjectAccessContext projectAccessContext) {
        this.selectionContext = selectionContext;
        this.projectAccessContext = projectAccessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MaterialSelectionItem> call(MaterialSelectionItem request) {
        Employee current = selectionContext.requireEmployee(employee);
        Project project = projectAccessContext.requireProject(projectId, current);
        List<MaterialSelectionItem> items = selectionContext.listProjectItems(project, current);
        return new PageImpl<>(items);
    }
}
