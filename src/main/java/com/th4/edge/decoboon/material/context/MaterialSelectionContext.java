package com.th4.edge.decoboon.material.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.model.MaterialSelectionItem;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 主材选材上下文：企业归属、房屋/合同/项目绑定与数据范围校验。 */
@Note("主材选材上下文")
@Component
public class MaterialSelectionContext {

    private final MaterialSelectionRepository repository;
    private final PropertyRepository propertyRepository;
    private final PropertyContext propertyContext;
    private final ProjectContractRepository contractRepository;
    private final ProjectRepository projectRepository;
    private final ContractLineRepository contractLineRepository;
    private final MaterialPlanRepository materialPlanRepository;

    public MaterialSelectionContext(MaterialSelectionRepository repository,
                                    PropertyRepository propertyRepository,
                                    PropertyContext propertyContext,
                                    ProjectContractRepository contractRepository,
                                    ProjectRepository projectRepository,
                                    ContractLineRepository contractLineRepository,
                                    MaterialPlanRepository materialPlanRepository) {
        this.repository = repository;
        this.propertyRepository = propertyRepository;
        this.propertyContext = propertyContext;
        this.contractRepository = contractRepository;
        this.projectRepository = projectRepository;
        this.contractLineRepository = contractLineRepository;
        this.materialPlanRepository = materialPlanRepository;
    }

    public Employee requireEmployee(Employee principal) {
        return propertyContext.requireEmployee(principal);
    }

    /** 维护动作的房屋校验：必须在房屋数据范围内且具备维护资格。 */
    public Property requireProperty(Long propertyId, Employee principal) {
        return propertyContext.require(propertyId, principal);
    }

    /** 查看动作的房屋校验：房屋数据范围或项目参与关系命中即可，越权统一 404。 */
    public Property requireViewableProperty(Long propertyId, Employee principal) {
        Employee current = requireEmployee(principal);
        if (propertyId == null) {
            throw new DefinedException(new DefaultErrorCode("404", "房屋不存在或无权访问"));
        }
        Property property = propertyRepository.findById(propertyId)
                .filter(item -> sameCompany(item.getCompany(), current))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "房屋不存在或无权访问")));
        if (!propertyContext.canView(property, current) && !isProjectParticipant(property, current)) {
            throw new DefinedException(new DefaultErrorCode("404", "房屋不存在或无权访问"));
        }
        return property;
    }

    /** 按主键加载选材记录并校验数据范围（越权统一 404，不泄露对象存在性）。 */
    public MaterialSelection require(Long selectionId, Employee principal) {
        Employee current = requireEmployee(principal);
        if (selectionId == null) {
            throw new DefinedException(new DefaultErrorCode("404", "选材记录不存在或无权访问"));
        }
        MaterialSelection selection = repository.findBySelectionId(selectionId)
                .filter(item -> sameCompany(item.getCompany(), current))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "选材记录不存在或无权访问")));
        requireViewable(selection, current);
        return selection;
    }

    /** 房屋数据范围或项目参与关系可见即允许查看选材文本。 */
    public void requireViewable(MaterialSelection selection, Employee current) {
        if (!canView(selection, current)) {
            throw new DefinedException(new DefaultErrorCode("404", "选材记录不存在或无权访问"));
        }
    }

    public boolean canView(MaterialSelection selection, Employee current) {
        if (selection == null || current == null) {
            return false;
        }
        if (propertyContext.canView(selection.getProperty(), current)) {
            return true;
        }
        // 项目参与人（设计师/项目经理/履约）按显式参与关系只读选材文本；
        // 项目可能在选材创建后才建立，因此按房屋维度查参与关系，而不是只看记录上的 project 引用。
        return isProjectParticipant(selection.getProperty(), current);
    }

    /** 维护与确认动作：Designer/Boss/StoreManager/Finance 在数据范围内可维护；Pm/TeamLeader 只读。 */
    public void requireMaintainer(Property property, Employee principal) {
        Employee current = requireEmployee(principal);
        if (canMaintain(property, current)) {
            return;
        }
        throw new DefinedException(new DefaultErrorCode("403", "只有该房屋的设计师、企业负责人、店长或财务可以维护主材选材"));
    }

    public boolean canMaintain(Property property, Employee current) {
        if (property == null || current == null || !propertyContext.canView(property, current)) {
            return false;
        }
        EmployeeRole role = current.getRole();
        if (role == EmployeeRole.Boss) {
            return true;
        }
        if (role == EmployeeRole.Designer) {
            return sameEmployee(property.getDesigner(), current) || sameEmployee(property.getOwner(), current);
        }
        // 店长和财务按数据范围维护；Pm、团队负责人和业务员只读。
        return role == EmployeeRole.StoreManager || role == EmployeeRole.Finance;
    }

    /** 解析并校验合同归属，合同必须属于当前企业且与房屋一致。 */
    public ProjectContract requireContract(Long contractId, Property property, Employee current) {
        if (contractId == null) {
            return null;
        }
        ProjectContract contract = contractRepository
                .findByContractIdAndCompany_ComId(contractId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "合同不存在或无权访问")));
        if (contract.getProperty() == null
                || !contract.getProperty().getPropertyId().equals(property.getPropertyId())) {
            throw new DefinedException(new DefaultErrorCode("400", "合同与所选房屋不匹配"));
        }
        return contract;
    }

    /** 解析并校验项目归属，项目必须属于当前企业且与房屋一致。 */
    public Project requireProject(Long projectId, Property property, Employee current) {
        if (projectId == null) {
            return null;
        }
        Project project = projectRepository.findById(projectId)
                .filter(item -> sameCompany(item.getCompany(), current))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "项目不存在或无权访问")));
        if (project.getProperty() == null
                || !project.getProperty().getPropertyId().equals(property.getPropertyId())) {
            throw new DefinedException(new DefaultErrorCode("400", "项目与所选房屋不匹配"));
        }
        return project;
    }

    /** 解析合同主材行，项目上下文必须与当前企业、房屋一致。 */
    public ContractLine requireContractLine(Long contractLineId, Project project, Employee principal) {
        Employee current = requireEmployee(principal);
        if (contractLineId == null) {
            return null;
        }
        ContractLine line = contractLineRepository.findById(contractLineId)
                .filter(item -> sameCompany(item.getCompany(), current))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "主材分项不存在或无权访问")));
        if (line.getType() != ItemType.Material) {
            throw new DefinedException(new DefaultErrorCode("400", "合同行不是主材分项"));
        }
        if (project != null && (line.getContract() == null || line.getContract().getProject() == null
                || !project.getProjectId().equals(line.getContract().getProject().getProjectId()))) {
            throw new DefinedException(new DefaultErrorCode("400", "主材分项与项目不匹配"));
        }
        return line;
    }

    /** 读取项目全部合同主材分项，并合并最新选材版本与材料计划下单状态。 */
    public List<MaterialSelectionItem> listProjectItems(Project project, Employee principal) {
        Employee current = requireEmployee(principal);
        if (project == null || project.getProjectId() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问"));
        }
        Long comId = project.getCompany() == null ? null : project.getCompany().getComId();
        List<ContractLine> lines = contractLineRepository
                .findByContract_Project_ProjectIdOrderBySortAsc(project.getProjectId()).stream()
                .filter(line -> line.getType() == ItemType.Material)
                .toList();
        Map<Long, MaterialSelection> latest = new HashMap<>();
        for (MaterialSelection selection : repository
                .findByCompany_ComIdAndProject_ProjectIdAndSourceContractLineIsNotNullOrderByVersionDesc(
                        comId, project.getProjectId())) {
            Long lineId = selection.getSourceContractLine() == null ? null : selection.getSourceContractLine().getLineId();
            if (lineId != null) {
                latest.putIfAbsent(lineId, selection);
            }
        }
        Map<Long, MaterialPlan> plans = new HashMap<>();
        for (MaterialPlan plan : materialPlanRepository.findByProject(project)) {
            if (plan.getContractLine() != null) {
                plans.putIfAbsent(plan.getContractLine().getLineId(), plan);
            }
        }
        List<MaterialSelectionItem> items = new ArrayList<>();
        for (ContractLine line : lines) {
            items.add(toItem(project, line, latest.get(line.getLineId()), plans.get(line.getLineId())));
        }
        return items;
    }

    /** 读取单个合同主材分项及其最新选材结果。 */
    public MaterialSelectionItem requireProjectItem(Project project, Long contractLineId, Employee principal) {
        Employee current = requireEmployee(principal);
        Project visible = project == null ? null : project;
        for (MaterialSelectionItem item : listProjectItems(visible, current)) {
            if (contractLineId != null && contractLineId.equals(item.getItemId())) {
                return item;
            }
        }
        throw new DefinedException(new DefaultErrorCode("404", "主材分项不存在或无权访问"));
    }

    private MaterialSelectionItem toItem(Project project, ContractLine line,
                                         MaterialSelection selection, MaterialPlan plan) {
        MaterialSelectionItem item = new MaterialSelectionItem();
        item.setItemId(line.getLineId());
        item.setProjectId(project.getProjectId());
        item.setProjectNo(project.getNo());
        item.setProjectName(project.getName());
        item.setPropertyId(project.getProperty() == null ? null : project.getProperty().getPropertyId());
        item.setPropertyAddress(project.getProperty() == null ? null : project.getProperty().getAddress());
        item.setCustomerName(project.getCustomer() == null ? null : project.getCustomer().getName());
        item.setContractId(line.getContract() == null ? null : line.getContract().getContractId());
        item.setContractNo(line.getContract() == null ? null : line.getContract().getNo());
        item.setSpaceName(line.getSpaceName());
        item.setCategory(line.getCategory());
        item.setMaterialName(line.getName());
        item.setBrand(line.getBrand());
        item.setSpec(line.getSpec());
        item.setUnit(line.getUnit() == null ? null : line.getUnit().getName());
        item.setQuantity(line.getQuantity());
        item.setSort(line.getSort());
        item.setRequired(true);
        item.setDesignerName(designerName(project, line, selection));
        if (selection != null) {
            item.setSelectionId(selection.getSelectionId());
            item.setVersion(selection.getVersion());
            item.setStatus(selection.getStatus() == null ? null : selection.getStatus().name());
            item.setStatusName(selection.getStatus() == null ? null : selection.getStatus().getName());
            item.setSelectionText(selection.getContent());
            item.setSelectionJson(selection.getSelectionJson());
            item.setAttachments(selection.getAttachments());
            item.setUpdatedAt(selection.getUpdateTime() == null ? null : selection.getUpdateTime().getDateTime());
        }
        applyOrderStatus(item, plan);
        item.setNextAction(nextAction(item));
        return item;
    }

    private String designerName(Project project, ContractLine line, MaterialSelection selection) {
        if (selection != null && selection.getDesigner() != null) {
            return selection.getDesigner().getName();
        }
        if (project.getDesigner() != null) {
            return project.getDesigner().getName();
        }
        if (project.getProperty() != null && project.getProperty().getDesigner() != null) {
            return project.getProperty().getDesigner().getName();
        }
        return null;
    }

    private void applyOrderStatus(MaterialSelectionItem item, MaterialPlan plan) {
        MaterialPlanStatus status = plan == null ? null : plan.getStatus();
        // 口径与 selectionLocked 保持一致：只要进入采购，App 就能识别并切换为"去发起增减项"
        if (status == MaterialPlanStatus.Requested || status == MaterialPlanStatus.Approved) {
            item.setOrderStatus("ordering");
            item.setOrderStatusName("已请购");
            return;
        }
        if (status == MaterialPlanStatus.PartiallyOrdered) {
            item.setOrderStatus("partiallyOrdered");
            item.setOrderStatusName("部分下单");
            return;
        }
        if (status == MaterialPlanStatus.Ordered) {
            item.setOrderStatus("ordered");
            item.setOrderStatusName("已下单");
            return;
        }
        if (status == MaterialPlanStatus.PartiallyArrived) {
            item.setOrderStatus("partiallyArrived");
            item.setOrderStatusName("部分到场");
            return;
        }
        if (status == MaterialPlanStatus.Arrived || status == MaterialPlanStatus.Closed) {
            item.setOrderStatus("arrived");
            item.setOrderStatusName("已到场");
            return;
        }
        if (status == MaterialPlanStatus.ReadyToOrder) {
            item.setOrderStatus("ready");
            item.setOrderStatusName("待财务下单");
            return;
        }
        item.setOrderStatus("none");
        item.setOrderStatusName("未下单");
    }

    /**
     * 计划行是否已进入采购或已结束（DEC-MP：下单后禁止再改选材）。
     *
     * <p>只有 {@code Planned}（待选材）与 {@code ReadyToOrder}（可下单）允许修改选材；
     * 一旦形成采购（已请购/已审批/部分下单/已下单/部分到场/已到场）或已关闭/取消，
     * 更换材料只能通过增减项（采购调整或退货单）处理。</p>
     */
    public boolean selectionLocked(MaterialPlan plan) {
        if (plan == null || plan.getStatus() == null) {
            return false;
        }
        return plan.getStatus() != MaterialPlanStatus.Planned
                && plan.getStatus() != MaterialPlanStatus.ReadyToOrder;
    }

    /** 同一合同行可能同时存在原计划与变更计划：任一条已进入采购即锁定选材。 */
    public boolean selectionLocked(List<MaterialPlan> plans) {
        return plans != null && plans.stream().anyMatch(this::selectionLocked);
    }

    /**
     * 下单前选材门禁（DEC-008 / DEC-056 / DEC-MP-05）：合同选材来源的计划行必须有"已选材（现场确认）"记录。
     *
     * <p>采购下单与作业单下单共用本方法，避免门禁出现两份实现而漂移。</p>
     */
    public void requireOrderableSelection(MaterialPlan plan) {
        if (plan == null) {
            throw new DefinedException(new DefaultErrorCode("400", "材料计划不存在"));
        }
        if (plan.getSourceType() != com.th4.edge.decoboon.project.configure.MaterialSourceType.Selection) {
            return;
        }
        MaterialSelection selection = plan.getSelection();
        if (selection == null && plan.getContractLine() != null && plan.getCompany() != null) {
            selection = repository
                    .findFirstByCompany_ComIdAndSourceContractLine_LineIdOrderByVersionDesc(
                            plan.getCompany().getComId(), plan.getContractLine().getLineId())
                    .orElse(null);
        }
        if (selection == null || selection.getStatus() != SelectionStatus.Selected) {
            throw new DefinedException(new DefaultErrorCode("400", "该材料尚未完成主材选材，完成后方可下单"));
        }
    }

    public void requireSelectionEditable(List<MaterialPlan> plans) {
        if (selectionLocked(plans)) {
            String lockedName = plans.stream().filter(this::selectionLocked)
                    .map(plan -> plan.getStatus() == null ? "已进入采购" : plan.getStatus().getName())
                    .findFirst().orElse("已进入采购");
            throw new DefinedException(new DefaultErrorCode("400",
                    "该材料已进入采购（" + lockedName + "），不能再修改选材；如需更换材料请发起增减项"));
        }
    }

    /** 按项目 + 合同主材分项取出全部相关材料计划行（无匹配时返回空列表）。 */
    public List<MaterialPlan> plansOf(Project project, ContractLine contractLine) {
        if (project == null || project.getProjectId() == null || contractLine == null) {
            return List.of();
        }
        return materialPlanRepository.findByProjectAndContractLine(project, contractLine);
    }

    private String nextAction(MaterialSelectionItem item) {
        if (item.getStatus() == null) {
            return "开始选材";
        }
        if ("ordered".equals(item.getOrderStatus()) || "partiallyArrived".equals(item.getOrderStatus())
                || "arrived".equals(item.getOrderStatus())) {
            return "查看选材与材料状态";
        }
        if ("ready".equals(item.getOrderStatus())) {
            return "等待财务下单";
        }
        return "交付财务下单";
    }

    public boolean sameCompany(Company company, Employee current) {
        return company != null && current.getCompany() != null
                && company.getComId().equals(current.getCompany().getComId());
    }

    private boolean isProjectParticipant(Property property, Employee current) {
        if (property == null || property.getPropertyId() == null
                || current.getCompany() == null || current.getEmpId() == null) {
            return false;
        }
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        Specification<Project> spec = (root, query, builder) -> builder.and(
                builder.equal(root.get("company").get("comId"), comId),
                builder.equal(root.get("property").get("propertyId"), property.getPropertyId()),
                builder.or(
                        builder.equal(root.get("designer").get("empId"), empId),
                        builder.equal(root.get("manager").get("empId"), empId),
                        builder.equal(root.get("support").get("empId"), empId)));
        return projectRepository.findOne(spec).isPresent();
    }

    private boolean sameEmployee(Employee target, Employee current) {
        return target != null && current != null && target.getEmpId() != null
                && target.getEmpId().equals(current.getEmpId());
    }
}
