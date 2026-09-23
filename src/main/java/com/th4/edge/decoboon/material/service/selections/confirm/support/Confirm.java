package com.th4.edge.decoboon.material.service.selections.confirm.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.context.MaterialPlanContext;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
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

import java.time.LocalDateTime;

/**
 * 记录客户对主材选材文本的确认。
 *
 * <p>确认动作只写入选材记录自身的状态、确认人姓名和确认时间，不改变报价、合同、成本、材料计划或采购事实；
 * 已确认版本再次调用直接返回原记录，保证重复确认幂等。</p>
 */
@Note("主材选材客户确认")
@Description("记录客户确认人与确认时间，已确认版本重复确认幂等，不覆盖历史也不回写金额")
@Properties(allow = {
        @AllowProperties(model = MaterialSelection.class, properties = {
                "selectionId", "version", "status", "title", "content", "attachments", "remark",
                "customer", "property", "contract", "project", "designer", "owner",
                "confirmerName", "confirmedBy", "confirmedAt", "createTime", "updateTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = ProjectContract.class, properties = {"contractId", "no", "baselineAmount", "signedAmount"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("material/selection/confirm")
@Scope(ScopeTypes.PROTOTYPE)
public class Confirm implements com.th4.edge.decoboon.material.service.selections.confirm.Confirm {

    @AutoInject @NotNull private Long selectionId;
    @AutoInject @NotEmpty @Length(max = 64) private String confirmerName;
    @AutoInject private LocalDateTime confirmedAt;
    @User private Employee employee;
    private final MaterialSelectionContext materialSelectionContext;
    private final MaterialSelectionRepository materialSelectionRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final MaterialPlanContext materialPlanContext;

    public Confirm(MaterialSelectionContext materialSelectionContext,
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
        MaterialSelection selection = materialSelectionContext.require(selectionId, current);
        materialSelectionContext.requireMaintainer(selection.getProperty(), current);
        if (selection.getStatus() == SelectionStatus.Confirmed) {
            // 幂等：重复确认不重写确认人、确认时间和历史版本。
            return selection;
        }
        // 门禁：材料已进入采购后不得再确认新版本选材（更换材料走增减项）
        materialSelectionContext.requireSelectionEditable(
                materialSelectionContext.plansOf(selection.getProject(), selection.getSourceContractLine()));
        if (selection.getContract() == null && selection.getProject() == null) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "请先绑定合同或项目上下文，再记录客户主材选材确认"));
        }
        selection.setStatus(SelectionStatus.Confirmed);
        selection.setConfirmerName(confirmerName);
        selection.setConfirmedBy(current);
        selection.setConfirmedAt(confirmedAt == null ? LocalDateTime.now() : confirmedAt);
        MaterialSelection saved = materialSelectionRepository.saveAndFlush(selection);
        promoteLinkedPlan(saved);
        return materialSelectionRepository.findBySelectionId(saved.getSelectionId()).orElse(saved);
    }

    /**
     * 选材客户确认后，将对应材料计划行推进为"可下单"并回填选材结果快照。
     *
     * <p>按 DEC-MP-05：合同选材确认后才允许财务下单；已下单及之后的计划不再回退状态。</p>
     */
    private void promoteLinkedPlan(MaterialSelection selection) {
        if (selection.getProject() == null || selection.getSourceContractLine() == null) {
            return;
        }
        materialPlanRepository
                .findFirstByProjectAndContractLine(selection.getProject(), selection.getSourceContractLine())
                .ifPresent(plan -> {
                    // 已进入采购的计划行不再回写选材快照：避免"计划行选材"与"采购单实际材料"口径不一致
                    if (materialSelectionContext.selectionLocked(plan)) {
                        return;
                    }
                    plan.setSelection(selection);
                    plan.setSelectedBrand(selection.getSourceContractLine().getBrand());
                    plan.setSelectedSpec(selection.getSourceContractLine().getSpec());
                    if (plan.getStatus() == null || plan.getStatus() == MaterialPlanStatus.Planned) {
                        plan.setStatus(MaterialPlanStatus.ReadyToOrder);
                    }
                    materialPlanContext.applyDerived(plan);
                    materialPlanRepository.save(plan);
                });
    }
}
