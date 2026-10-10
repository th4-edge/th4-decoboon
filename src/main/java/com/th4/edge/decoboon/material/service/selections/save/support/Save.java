package com.th4.edge.decoboon.material.service.selections.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.context.MaterialPlanContext;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 保存签约后主材选材结果（现场代录）。
 *
 * <p>业务口径（SCN-006 / BR-004 / DEC-003 / DEC-056）：设计师现场或逐项沟通与客户确认后，
 * 由具备维护权限的角色代录选材结果，保存即"已选材（现场确认）"，不再有草稿/待确认/客户确认动作；
 * 选材结果以文本保存，不拆逐材料结构化字段；保存动作不改变报价、合同、成本、材料计划和采购事实。</p>
 *
 * <p>当 {@code deliver=true} 时，将对应材料计划行推进为 {@code ReadyToOrder}（待财务下单）；
 * 交付不等于下单，供应商/价格/数量仍由财务在下单时决定（DEC-MP-03）。</p>
 */
@Note("主材选材记录保存")
@Description("设计师现场代录主材选材结果；可选交付财务下单，将材料计划行推进为可下单")
@Properties(allow = {
        @AllowProperties(model = MaterialSelection.class, properties = {
                "selectionId", "version", "status", "title", "content", "selectionJson", "attachments", "remark",
                "customer", "property", "contract", "project", "designer", "owner",
                "createTime", "updateTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = ProjectContract.class, properties = {"contractId", "no", "baselineAmount", "signedAmount"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("material/selection/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.material.service.selections.save.Save {

    @AutoInject @NotNull private Long propertyId;
    @AutoInject private Long contractId;
    @AutoInject private Long projectId;
    @AutoInject private Long contractLineId;
    @AutoInject @Length(max = 128) private String title;
    @AutoInject @NotEmpty private String content;
    @AutoInject private String selectionJson;
    @AutoInject private List<String> attachments;
    @AutoInject @Length(max = 500) private String remark;
    /** 是否同时交付财务下单：true 时把材料计划行由 Planned 推进为 ReadyToOrder。 */
    @AutoInject private Boolean deliver;
    @User private Employee employee;
    private final MaterialSelectionContext materialSelectionContext;
    private final MaterialSelectionRepository materialSelectionRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final MaterialPlanContext materialPlanContext;

    public Save(MaterialSelectionContext materialSelectionContext,
                MaterialSelectionRepository materialSelectionRepository,
                MaterialPlanRepository materialPlanRepository,
                MaterialPlanContext materialPlanContext) {
        this.materialSelectionContext = materialSelectionContext;
        this.materialSelectionRepository = materialSelectionRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.materialPlanContext = materialPlanContext;
    }

    @Override
    @Transactional
    public MaterialSelection call(MaterialSelection request) {
        Employee current = materialSelectionContext.requireEmployee(employee);
        if (content == null || content.trim().isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "请填写主材选材文本后再保存"));
        }
        Property property = materialSelectionContext.requireProperty(propertyId, current);
        materialSelectionContext.requireMaintainer(property, current);
        ProjectContract contract = materialSelectionContext.requireContract(contractId, property, current);
        Project project = materialSelectionContext.requireProject(projectId, property, current);
        ContractLine contractLine = materialSelectionContext.requireContractLine(contractLineId, project, current);
        if (contractLine != null) {
            contract = contractLine.getContract();
            project = contract == null ? project : contract.getProject();
        }
        if (contract == null && project != null && project.getContract() != null) {
            // 项目创建后自动带出履约合同，保持 Property + Contract 主归属一致。
            contract = project.getContract();
        }
        // 门禁：材料已进入采购（已请购/已下单/已到场等）后不得再改选材，更换材料必须走增减项
        List<MaterialPlan> plans = materialSelectionContext.plansOf(project, contractLine);
        materialSelectionContext.requireSelectionEditable(plans);

        Long comId = property.getCompany().getComId();
        MaterialSelection ongoing;
        if (contractLine != null) {
            ongoing = materialSelectionRepository
                    .findFirstByCompany_ComIdAndSourceContractLine_LineIdOrderByVersionDesc(
                            comId, contractLine.getLineId()).orElse(null);
        } else {
            ongoing = contract == null
                    ? materialSelectionRepository
                    .findFirstByCompany_ComIdAndProperty_PropertyIdAndContractIsNullOrderByVersionDesc(
                            comId, property.getPropertyId()).orElse(null)
                    : materialSelectionRepository
                    .findFirstByCompany_ComIdAndProperty_PropertyIdAndContract_ContractIdOrderByVersionDesc(
                            comId, property.getPropertyId(), contract.getContractId()).orElse(null);
        }
        // DEC-056：进入采购前允许继续修改；每次保存追加新版本，历史版本只读保留。
        MaterialSelection target = new MaterialSelection();
        target.setVersion(ongoing == null || ongoing.getVersion() == null
                ? 1 : ongoing.getVersion() + 1);
        target.setCompany(property.getCompany());
        target.setStore(property.getStore());
        target.setTeam(property.getTeam());
        target.setOwner(property.getOwner());
        target.setCustomer(property.getCustomer());
        target.setProperty(property);
        target.setContract(contract);
        target.setProject(project);
        target.setSourceContractLine(contractLine);
        target.setDesigner(resolveDesigner(property, current));
        target.setStatus(SelectionStatus.Selected);
        target.setTitle(title == null || title.isBlank() ? (contractLine == null ? title : contractLine.getName()) : title);
        target.setContent(content);
        target.setSelectionJson(selectionJson);
        target.setAttachments(attachments);
        target.setRemark(remark);
        MaterialSelection saved = materialSelectionRepository.saveAndFlush(target);
        syncLinkedPlan(saved, plans, Boolean.TRUE.equals(deliver));
        return materialSelectionRepository.findBySelectionId(saved.getSelectionId()).orElse(saved);
    }

    /**
     * 回填材料计划行的选材快照；交付财务时把 Planned 推进为 ReadyToOrder。
     *
     * <p>已进入采购的计划行不回写快照（selectionLocked 已在入口拦截，这里再做一层防御）。</p>
     */
    private void syncLinkedPlan(MaterialSelection selection, List<MaterialPlan> plans, boolean deliver) {
        if (plans == null || plans.isEmpty()) {
            return;
        }
        for (MaterialPlan plan : plans) {
            if (materialSelectionContext.selectionLocked(plan)) {
                continue;
            }
            plan.setSelection(selection);
            if (selection.getSourceContractLine() != null) {
                plan.setSelectedBrand(selection.getSourceContractLine().getBrand());
                plan.setSelectedSpec(selection.getSourceContractLine().getSpec());
            }
            if (deliver && (plan.getStatus() == null || plan.getStatus() == MaterialPlanStatus.Planned)) {
                plan.setStatus(MaterialPlanStatus.ReadyToOrder);
            }
            materialPlanContext.applyDerived(plan);
            materialPlanRepository.save(plan);
        }
    }

    /** 选材事实的设计责任人：优先取房屋已指派设计师，未指派时记为当前维护人。 */
    private Employee resolveDesigner(Property property, Employee current) {
        if (property.getDesigner() != null) {
            return property.getDesigner();
        }
        return property.getCustomer() != null && property.getCustomer().getDesigner() != null
                ? property.getCustomer().getDesigner() : current;
    }
}
