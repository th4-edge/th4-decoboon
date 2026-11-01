package com.th4.edge.decoboon.material.service.selections.board.support;

import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.model.MaterialSelectionItem;
import com.th4.edge.decoboon.material.model.SelectionBoardItem;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 主材选材跟进台。
 *
 * <p>按当前员工数据范围取出「已签约进入履约」的在跑项目，左连该房屋最新一版选材记录，
 * 只返回尚未完成逐项选材的项目，并给出责任设计师与下一步动作（DEC-056：现场代录，无客户确认动作）。</p>
 */
@Note("主材选材跟进台")
@Description("按数据范围列出尚未客户确认主材选材的在跑项目，含当前状态、责任设计师与下一步")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.decoboon.material.model.SelectionBoardItem.class,
                properties = {"projectId", "projectNo", "projectName", "projectStatus", "projectStatusName", "propertyId", "propertyAddress", "community", "area", "customerName", "designerName", "selectionId", "selectionStatus", "selectionStatusName", "totalItems", "selectedItems", "deliveredItems", "notSelectedItems", "orderedItems", "progress", "version", "updatedAt", "nextAction"})
})
@Service("material/selection/board")
@Scope(ScopeTypes.PROTOTYPE)
public class Board implements com.th4.edge.decoboon.material.service.selections.board.Board {

    /** 视为「在跑」的项目状态：已签约进入履约、尚未完工结算。 */
    private static final Set<ProjectStatus> RUNNING_STATUSES = EnumSet.of(
            ProjectStatus.Preparing, ProjectStatus.Ongoing, ProjectStatus.Suspended, ProjectStatus.Accepting);

    @User private Employee employee;
    private final MaterialSelectionContext materialSelectionContext;
    private final ProjectAccessContext projectAccessContext;
    private final ProjectRepository projectRepository;
    private final MaterialSelectionRepository materialSelectionRepository;

    public Board(MaterialSelectionContext materialSelectionContext,
                 ProjectAccessContext projectAccessContext,
                 ProjectRepository projectRepository,
                 MaterialSelectionRepository materialSelectionRepository) {
        this.materialSelectionContext = materialSelectionContext;
        this.projectAccessContext = projectAccessContext;
        this.projectRepository = projectRepository;
        this.materialSelectionRepository = materialSelectionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SelectionBoardItem> call(MaterialSelection request) {
        Employee current = materialSelectionContext.requireEmployee(employee);
        List<Project> projects = projectRepository.findAll(
                projectAccessContext.visibleProject(current)
                        .and((root, query, builder) -> root.get("status").in(RUNNING_STATUSES)));
        if (projects.isEmpty()) {
            return List.of();
        }
        List<Long> propertyIds = new ArrayList<>();
        for (Project project : projects) {
            Property property = project.getProperty();
            if (property != null && property.getPropertyId() != null && !propertyIds.contains(property.getPropertyId())) {
                propertyIds.add(property.getPropertyId());
            }
        }
        Map<Long, MaterialSelection> latestByProperty = new HashMap<>();
        if (!propertyIds.isEmpty()) {
            Long comId = current.getCompany() == null ? null : current.getCompany().getComId();
            for (MaterialSelection selection : materialSelectionRepository
                    .findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(comId, propertyIds)) {
                Long key = selection.getProperty().getPropertyId();
                // 仓储已按 version 倒序，首次出现即最新版本。
                latestByProperty.putIfAbsent(key, selection);
            }
        }
        List<SelectionBoardItem> items = new ArrayList<>();
        for (Project project : projects) {
            Property property = project.getProperty();
            if (property == null || property.getPropertyId() == null) {
                continue;
            }
            MaterialSelection selection = latestByProperty.get(property.getPropertyId());
            List<MaterialSelectionItem> materialItems = materialSelectionContext.listProjectItems(project, current);
            int totalItems = materialItems.size();
            int selectedItems = 0;
            int deliveredItems = 0;
            int orderedItems = 0;
            for (MaterialSelectionItem materialItem : materialItems) {
                if (materialItem.getStatus() != null) {
                    selectedItems++;
                }
                if (!"none".equals(materialItem.getOrderStatus())) {
                    deliveredItems++;
                }
                if (isPurchasing(materialItem.getOrderStatus())) {
                    orderedItems++;
                }
            }
            if (totalItems > 0 && selectedItems >= totalItems) {
                continue;
            }
            SelectionBoardItem item = toItem(project, property, selection);
            item.setTotalItems(totalItems);
            item.setSelectedItems(selectedItems);
            item.setDeliveredItems(deliveredItems);
            item.setNotSelectedItems(Math.max(0, totalItems - selectedItems));
            item.setOrderedItems(orderedItems);
            item.setProgress(totalItems > 0 ? (int) Math.round(selectedItems * 100.0 / totalItems) : 0);
            if (totalItems == 0) {
                item.setSelectionStatusName("未配置主材");
                item.setNextAction("检查合同主材分项");
            } else if (selectedItems > 0) {
                item.setSelectionStatusName("选材中");
                item.setNextAction("完善选材并交付财务下单");
            } else {
                item.setSelectionStatusName("未发起");
                item.setNextAction("开始逐项主材选材");
            }
            items.add(item);
        }
        return items;
    }

    private SelectionBoardItem toItem(Project project, Property property, MaterialSelection selection) {
        SelectionBoardItem item = new SelectionBoardItem();
        item.setProjectId(project.getProjectId());
        item.setProjectNo(project.getNo());
        item.setProjectName(project.getName());
        item.setProjectStatus(project.getStatus() == null ? null : project.getStatus().name());
        item.setProjectStatusName(project.getStatus() == null ? null : project.getStatus().getName());
        item.setPropertyId(property.getPropertyId());
        item.setPropertyAddress(property.getAddress());
        item.setCommunity(property.getCommunity());
        item.setArea(property.getArea());
        Customer customer = project.getCustomer();
        item.setCustomerName(customer == null ? null : customer.getName());
        item.setDesignerName(resolveDesignerName(property, project));
        if (selection == null) {
            item.setSelectionStatusName("未创建");
            item.setNextAction("发起主材选材");
            return item;
        }
        SelectionStatus status = selection.getStatus();
        item.setSelectionId(selection.getSelectionId());
        item.setSelectionStatus(status == null ? null : status.name());
        item.setSelectionStatusName(status == null ? "未创建" : status.getName());
        item.setVersion(selection.getVersion());
        item.setUpdatedAt(selection.getUpdateTime() == null ? null : selection.getUpdateTime().getDateTime());
        item.setNextAction("完善选材并交付财务下单");
        return item;
    }

    /** 是否已进入采购（用于跟进台区分已交付与已下单）。 */
    private boolean isPurchasing(String orderStatus) {
        return "ordering".equals(orderStatus) || "partiallyOrdered".equals(orderStatus)
                || "ordered".equals(orderStatus) || "partiallyArrived".equals(orderStatus)
                || "arrived".equals(orderStatus);
    }

    /** 选材责任设计师：优先房屋已指派设计师，其次项目设计师。 */
    private String resolveDesignerName(Property property, Project project) {
        if (property.getDesigner() != null) {
            return property.getDesigner().getName();
        }
        return project.getDesigner() == null ? null : project.getDesigner().getName();
    }
}
