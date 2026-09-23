package com.th4.edge.decoboon.material.service.selections.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
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
 * 保存签约后主材选材文本。
 *
 * <p>业务口径（SCN-006 / BR-004 / DEC-003）：选材结果以文本保存，不拆逐材料结构化字段；
 * 同一房屋与合同下保留历史版本，已确认版本不被覆盖，需要变更时形成新版本；
 * 保存动作不改变报价、合同、成本、材料计划和采购事实。</p>
 */
@Note("主材选材记录保存")
@Description("设计师整理主材选材文本并保存，进行中版本原地保存，已确认版本后变更形成新版本")
@Properties(allow = {
        @AllowProperties(model = MaterialSelection.class, properties = {
                "selectionId", "version", "status", "title", "content", "selectionJson", "attachments", "remark",
                "customer", "property", "contract", "project", "designer", "owner",
                "confirmerName", "confirmedBy", "confirmedAt", "createTime", "updateTime"}),
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
    @AutoInject private SelectionStatus status;
    @User private Employee employee;
    private final MaterialSelectionContext materialSelectionContext;
    private final MaterialSelectionRepository materialSelectionRepository;

    public Save(MaterialSelectionContext materialSelectionContext,
                MaterialSelectionRepository materialSelectionRepository) {
        this.materialSelectionContext = materialSelectionContext;
        this.materialSelectionRepository = materialSelectionRepository;
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
        materialSelectionContext.requireSelectionEditable(
                materialSelectionContext.plansOf(project, contractLine));
        SelectionStatus targetStatus = status == SelectionStatus.PendingConfirm
                ? SelectionStatus.PendingConfirm : SelectionStatus.Draft;
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
        MaterialSelection target;
        if (ongoing != null && !ongoing.getStatus().locked()) {
            // 未确认的当前版本允许原地保存，避免每次编辑都堆积版本。
            target = ongoing;
        } else {
            target = new MaterialSelection();
            target.setVersion((ongoing == null ? 0 : ongoing.getVersion()) + 1);
        }
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
        target.setStatus(targetStatus);
        target.setTitle(title == null || title.isBlank() ? (contractLine == null ? title : contractLine.getName()) : title);
        target.setContent(content);
        target.setSelectionJson(selectionJson);
        target.setAttachments(attachments);
        target.setRemark(remark);
        MaterialSelection saved = materialSelectionRepository.saveAndFlush(target);
        return materialSelectionRepository.findBySelectionId(saved.getSelectionId()).orElse(saved);
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
