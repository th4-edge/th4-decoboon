package com.th4.edge.decoboon.project.service.contractchange.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.context.CarryoverLockContext;
import com.th4.edge.decoboon.project.context.ContractChangeAmountContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 发起增项、减项、替换、数量或价格变更及其报价明细。 */
@Note("发起合同变更")
@Description("审批前生成变更报价明细，客户确认后才同步合同和应收")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "no", "reason", "type", "sourceLine",
                        "amount", "costDelta", "status", "customerConfirmed", "customerConfirmedAt", "customerConfirmedBy", "customerConfirmMethod",
                        "evidenceImages", "signatureUrl", "items", "requestedBy", "approvedBy",
                        "conclusion", "approvedAt", "createTime"}),
        @AllowProperties(model = ContractChangeItem.class,
                properties = {"changeItemId", "sourceLine", "changeType", "itemType", "name",
                        "oldQuantity", "newQuantity", "oldPrice", "newPrice", "amountDelta", "costDelta", "remark"}),
        @AllowProperties(model = ContractLine.class,
                properties = {"lineId", "type", "name", "quantity", "materialPrice", "manualPrice", "amount", "cost"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "signedAt"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "status", "property"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract-change/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.contractchange.save.Save {

    @AutoInject @NotNull private Long contractId;
    @AutoInject @NotEmpty private String reason;
    @AutoInject private BigDecimal amount;
    @AutoInject private ContractChangeType type;
    @AutoInject private Long contractLineId;
    @AutoInject private BigDecimal newQuantity;
    @AutoInject private BigDecimal newPrice;
    @AutoInject private BigDecimal newCost;
    @AutoInject private BigDecimal costDelta;
    @AutoInject private List<String> evidenceImages;
    @AutoInject private String signatureUrl;
    @AutoInject private LocalDateTime customerConfirmedAt;
    @AutoInject private String customerConfirmedBy;
    @AutoInject private String customerConfirmMethod;
    @AutoInject private Long reportId;
    @User private Employee employee;

    private final ProjectContractRepository contractRepository;
    private final ContractChangeRepository changeRepository;
    private final ContractChangeItemRepository itemRepository;
    private final ContractChangeAmountContext amountContext;
    private final ProjectRepository projectRepository;
    private final ProjectReportRepository reportRepository;
    private final ProjectAccessContext accessContext;
    private final ModelIdentityContext modelIdentityContext;
    private final ProjectPermissionContext permissionContext;
    private final CarryoverLockContext carryoverLockContext;

    public Save(ProjectContractRepository contractRepository,
                ContractChangeRepository changeRepository,
                ContractChangeItemRepository itemRepository,
                ContractChangeAmountContext amountContext,
                ProjectRepository projectRepository,
                ProjectReportRepository reportRepository,
                ProjectAccessContext accessContext,
                ModelIdentityContext modelIdentityContext,
                ProjectPermissionContext permissionContext,
                CarryoverLockContext carryoverLockContext) {
        this.contractRepository = contractRepository;
        this.changeRepository = changeRepository;
        this.itemRepository = itemRepository;
        this.amountContext = amountContext;
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.accessContext = accessContext;
        this.modelIdentityContext = modelIdentityContext;
        this.permissionContext = permissionContext;
        this.carryoverLockContext = carryoverLockContext;
    }

    @Override
    @Transactional
    public ContractChange call(ContractChange request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<ProjectContract> contractById = (root, query, builder) ->
                builder.equal(root.get("contractId"), contractId);
        ProjectContract contract = contractRepository.findOne(
                        contractById.and(accessContext.visibleContract(employee)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合同基线不存在或无权访问")));
        if (contract.getProject() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "合同尚未生成项目"));
        }
        Project project = accessContext.requireProject(contract.getProject().getProjectId(), current);
        // 增减项发起：企业负责人 / 店长 / 项目经理 / 项目设计师（用户 2026-09-23 裁决）
        accessContext.requireChangeInitiator(project, current);
        // 增减项只能在项目未完工前发起：完工/已结算后不允许再改合同与账务（合同终止/关闭同样阻断）。
        if (project.getStatus() == com.th4.edge.decoboon.project.configure.ProjectStatus.Finished
                || project.getStatus() == com.th4.edge.decoboon.project.configure.ProjectStatus.Settled) {
            throw new DefinedException(new DefaultErrorCode("400", "项目已完工，不能继续发起增减项"));
        }
        if (contract.getStatus() == com.th4.edge.decoboon.project.configure.ContractStatus.Closed
                || contract.getStatus() == com.th4.edge.decoboon.project.configure.ContractStatus.Terminated) {
            throw new DefinedException(new DefaultErrorCode("400", "合同已终止或关闭，不能继续发起增减项"));
        }
        ContractChangeType changeType = type == null ? ContractChangeType.Add : type;
        ContractLine sourceLine = amountContext.line(contract, contractLineId);
        ContractChangeAmountContext.Computed computed = amountContext.compute(
                changeType, sourceLine, amount, costDelta, newQuantity, newPrice, newCost);
        BigDecimal amountDelta = computed.amountDelta();
        BigDecimal costChange = computed.costDelta();
        if (amountDelta.signum() == 0 && costChange.signum() == 0) {
            throw new DefinedException(new DefaultErrorCode("400", "变更金额或成本不能同时为 0"));
        }
        requireCustomerSignOff();

        request.setCompany(project.getCompany());
        request.setContract(contract);
        request.setProject(project);
        request.setProperty(project.getProperty());
        permissionContext.apply(request, project);
        request.setNo("BC" + System.currentTimeMillis());
        request.setReason(reason.trim());
        request.setType(changeType);
        request.setSourceLine(sourceLine);
        request.setAmount(amountDelta);
        request.setCostDelta(costChange);
        request.setStatus(ContractChangeStatus.Pending);
        // 新口径（DEC-041）：客户线下确认是创建前置事实，由设计师登记；企业审批通过即生效。
        request.setCustomerConfirmed(true);
        request.setCustomerConfirmedAt(customerConfirmedAt);
        request.setCustomerConfirmedBy(customerConfirmedBy.trim());
        request.setCustomerConfirmMethod(customerConfirmMethod.trim());
        request.setRequestedBy(current);
        request.setEvidenceImages(evidenceImages);
        request.setSignatureUrl(signatureUrl);
        ContractChange saved = changeRepository.save(modelIdentityContext.prepare(request));

        ContractChangeItem item = changeItem(saved, sourceLine, changeType, computed);
        itemRepository.save(modelIdentityContext.prepare(item));
        saved.setItems(List.of(item));
        if (reportId != null) {
            ProjectReport report = reportRepository.findOne(
                            ((Specification<ProjectReport>) (root, query, builder) -> builder.equal(root.get("reportId"), reportId))
                                    .and(accessContext.visibleChild(employee, ProjectReport.class, "project")))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "现场汇报不存在或无权访问")));
            report.setChange(saved);
            reportRepository.save(report);
        }
        if (carryoverLockContext != null) carryoverLockContext.lock(project, CarryoverBusinessType.ContractChange, current);
        return saved;
    }

    /** DEC-041/BR-035：无客户线下确认时间、确认人、方式与签认证据，不得形成增减项变更单。 */
    private void requireCustomerSignOff() {
        if (customerConfirmedAt == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请登记客户线下确认时间"));
        }
        if (customerConfirmedBy == null || customerConfirmedBy.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请登记客户确认人"));
        }
        if (customerConfirmMethod == null || customerConfirmMethod.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择客户确认方式"));
        }
        boolean hasEvidence = (evidenceImages != null && !evidenceImages.isEmpty())
                || (signatureUrl != null && !signatureUrl.isBlank());
        if (!hasEvidence) {
            throw new DefinedException(new DefaultErrorCode("400", "请上传客户签认证据"));
        }
    }

    private ContractChangeItem changeItem(ContractChange change, ContractLine source, ContractChangeType type,
                                          ContractChangeAmountContext.Computed computed) {
        ContractChangeItem item = new ContractChangeItem();
        item.setCompany(change.getCompany());
        item.setChange(change);
        item.setSourceLine(source);
        item.setChangeType(type);
        item.setItemType(source == null ? null : source.getType());
        item.setName(source == null ? change.getReason() : source.getName());
        item.setOldQuantity(computed.oldQuantity());
        item.setNewQuantity(computed.newQuantity());
        item.setOldPrice(computed.oldPrice());
        item.setNewPrice(computed.newPrice());
        item.setAmountDelta(computed.amountDelta());
        item.setCostDelta(computed.costDelta());
        item.setRemark(change.getReason());
        return item;
    }

    private BigDecimal value(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
