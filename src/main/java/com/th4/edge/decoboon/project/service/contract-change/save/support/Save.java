package com.th4.edge.decoboon.project.service.contractchange.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
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
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
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
import java.util.List;

/** 发起增项、减项、替换、数量或价格变更及其报价明细。 */
@Note("发起合同变更")
@Description("审批前生成变更报价明细，客户确认后才同步合同和应收")
@Properties(allow = {
        @AllowProperties(model = ContractChange.class,
                properties = {"changeId", "contract", "project", "no", "reason", "type", "sourceLine",
                        "amount", "costDelta", "status", "customerConfirmed", "customerConfirmedAt",
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
    @AutoInject private Long reportId;
    @User private Employee employee;

    private final ProjectContractRepository contractRepository;
    private final ContractChangeRepository changeRepository;
    private final ContractChangeItemRepository itemRepository;
    private final ContractLineRepository lineRepository;
    private final ProjectRepository projectRepository;
    private final ProjectReportRepository reportRepository;
    private final ProjectAccessContext accessContext;
    private final ModelIdentityContext modelIdentityContext;
    private final ProjectPermissionContext permissionContext;

    public Save(ProjectContractRepository contractRepository,
                ContractChangeRepository changeRepository,
                ContractChangeItemRepository itemRepository,
                ContractLineRepository lineRepository,
                ProjectRepository projectRepository,
                ProjectReportRepository reportRepository,
                ProjectAccessContext accessContext,
                ModelIdentityContext modelIdentityContext,
                ProjectPermissionContext permissionContext) {
        this.contractRepository = contractRepository;
        this.changeRepository = changeRepository;
        this.itemRepository = itemRepository;
        this.lineRepository = lineRepository;
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.accessContext = accessContext;
        this.modelIdentityContext = modelIdentityContext;
        this.permissionContext = permissionContext;
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
        ContractLine sourceLine = null;
        if (contractLineId != null) {
            Specification<ContractLine> lineById = (root, query, builder) ->
                    builder.equal(root.get("lineId"), contractLineId);
            sourceLine = lineRepository.findOne(lineById.and((root, query, builder) -> builder.equal(
                            root.get("contract").get("contractId"), contract.getContractId())))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "原合同行不存在")));
        }
        BigDecimal amountDelta = resolveAmountDelta(changeType, sourceLine, amount, newQuantity, newPrice);
        BigDecimal costChange = resolveCostDelta(changeType, sourceLine, costDelta, newQuantity, newPrice, newCost);
        if (amountDelta.signum() == 0 && costChange.signum() == 0) {
            throw new DefinedException(new DefaultErrorCode("400", "变更金额或成本不能同时为 0"));
        }

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
        request.setCustomerConfirmed(false);
        request.setRequestedBy(current);
        request.setEvidenceImages(evidenceImages);
        request.setSignatureUrl(signatureUrl);
        ContractChange saved = changeRepository.save(modelIdentityContext.prepare(request));

        ContractChangeItem item = changeItem(saved, sourceLine, changeType, newQuantity, newPrice, amountDelta, costChange);
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
        return saved;
    }

    private ContractChangeItem changeItem(ContractChange change, ContractLine source, ContractChangeType type,
                                          BigDecimal newQuantityValue, BigDecimal newPriceValue,
                                          BigDecimal amountDelta, BigDecimal costChange) {
        BigDecimal oldQuantity = source == null ? BigDecimal.ZERO : value(source.getQuantity());
        BigDecimal oldUnitPrice = source == null || oldQuantity.signum() == 0
                ? BigDecimal.ZERO : value(source.getAmount()).divide(oldQuantity, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal newQuantity = newQuantityValue == null ? oldQuantity : newQuantityValue;
        BigDecimal newPrice = newPriceValue == null ? oldUnitPrice : newPriceValue;
        ContractChangeItem item = new ContractChangeItem();
        item.setCompany(change.getCompany());
        item.setChange(change);
        item.setSourceLine(source);
        item.setChangeType(type);
        item.setItemType(source == null ? null : source.getType());
        item.setName(source == null ? change.getReason() : source.getName());
        item.setOldQuantity(oldQuantity);
        item.setNewQuantity(newQuantity);
        item.setOldPrice(oldUnitPrice);
        item.setNewPrice(newPrice);
        item.setAmountDelta(amountDelta);
        item.setCostDelta(costChange);
        item.setRemark(change.getReason());
        return item;
    }

    private BigDecimal resolveAmountDelta(ContractChangeType type, ContractLine source,
                                          BigDecimal requested, BigDecimal newQuantity, BigDecimal newPrice) {
        if (requested != null) return requested;
        if (source == null) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少原合同行，无法计算变更金额"));
        }
        if (type == ContractChangeType.Quantity || type == ContractChangeType.Replace) {
            BigDecimal quantity = newQuantity == null ? source.getQuantity() : newQuantity;
            BigDecimal unitPrice = newPrice != null ? newPrice : unitPrice(source);
            return quantity.subtract(value(source.getQuantity())).multiply(unitPrice);
        }
        if (type == ContractChangeType.Price) {
            BigDecimal price = newPrice == null ? unitPrice(source) : newPrice;
            return price.subtract(unitPrice(source)).multiply(value(source.getQuantity()));
        }
        throw new DefinedException(new DefaultErrorCode("400", "请填写变更金额"));
    }

    private BigDecimal resolveCostDelta(ContractChangeType type, ContractLine source, BigDecimal requested,
                                        BigDecimal newQuantity, BigDecimal newPrice, BigDecimal newCost) {
        if (requested != null) return requested;
        if (source == null || source.getQuantity() == null || source.getQuantity().signum() == 0) return BigDecimal.ZERO;
        BigDecimal oldUnitCost = value(source.getCost()).divide(source.getQuantity(), 2, java.math.RoundingMode.HALF_UP);
        if (type == ContractChangeType.Quantity || type == ContractChangeType.Replace) {
            BigDecimal quantity = newQuantity == null ? source.getQuantity() : newQuantity;
            return quantity.subtract(value(source.getQuantity())).multiply(oldUnitCost);
        }
        if (type == ContractChangeType.Price) {
            BigDecimal cost = newCost == null ? oldUnitCost : newCost;
            return cost.subtract(oldUnitCost).multiply(value(source.getQuantity()));
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal unitPrice(ContractLine source) {
        if (source == null || source.getQuantity() == null || source.getQuantity().signum() == 0) return BigDecimal.ZERO;
        return value(source.getAmount()).divide(source.getQuantity(), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
