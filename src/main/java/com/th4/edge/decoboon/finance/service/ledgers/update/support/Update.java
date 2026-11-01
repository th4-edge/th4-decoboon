package com.th4.edge.decoboon.finance.service.ledgers.update.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.LedgerManualPurpose;
import com.th4.edge.decoboon.finance.configure.LedgerRevisionType;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.LedgerCategoryContext;
import com.th4.edge.decoboon.finance.context.ManualLedgerContext;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.decoboon.finance.model.LedgerRevision;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.LedgerRevisionRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
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
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** 手工流水元数据更正：支持文本分类、账务用途与历史分类节点兼容。 */
@Note("更正手工流水")
@Properties(allow = {
        @AllowProperties(model = Ledger.class,
                properties = {"ledgerId", "project", "direction", "category", "categoryNode", "purpose", "source", "amount", "account",
                        "occurredAt", "counterparty", "summary", "remark", "originalLedger", "correctionGroupId", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = LedgerCategory.class,
                properties = {"categoryId", "name", "description", "direction", "code", "builtIn", "sort", "active"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class, properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"})
})
@Service("finance/ledger/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.finance.service.ledgers.update.Update {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long ledgerId;
    /** 兼容旧版 App 的分类节点；新版本不再传。 */
    @AutoInject
    private Long categoryId;
    @AutoInject
    private LedgerManualPurpose purpose;
    @AutoInject
    @Length(max = 128)
    private String category;
    @AutoInject
    @Length(max = 128)
    private String counterparty;
    @AutoInject
    @Length(max = 256)
    private String summary;
    @AutoInject
    @Length(max = 500)
    private String remark;
    @AutoInject
    private Long projectId;
    @AutoInject
    @NotEmpty
    @Length(max = 500)
    private String reason;

    private final LedgerRepository ledgerRepository;
    private final LedgerRevisionRepository revisionRepository;
    private final LedgerCategoryContext categoryContext;
    private final ManualLedgerContext manualContext;
    private final FinanceAccessContext accessContext;
    private final ObjectMapper objectMapper;

    public Update(LedgerRepository ledgerRepository, LedgerRevisionRepository revisionRepository,
                  LedgerCategoryContext categoryContext, ManualLedgerContext manualContext,
                  FinanceAccessContext accessContext, ObjectMapper objectMapper) {
        this.ledgerRepository = ledgerRepository;
        this.revisionRepository = revisionRepository;
        this.categoryContext = categoryContext;
        this.manualContext = manualContext;
        this.accessContext = accessContext;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Ledger call(Ledger request) {
        Employee current = accessContext.requireEditor(employee);
        Ledger ledger = requireManual(current, ledgerId);
        boolean boss = current.getRole() == EmployeeRole.Boss;
        if (!boss && (changed(counterparty, ledger.getCounterparty())
                || changed(summary, ledger.getSummary())
                || changed(remark, ledger.getRemark())
                || projectChanged(projectId, ledger.getProject()))) {
            throw new DefinedException(new DefaultErrorCode("403", "财务仅可更正分类与账务用途，其他元数据更正需企业负责人操作"));
        }
        if (categoryId == null && purpose == null && category == null
                && counterparty == null && summary == null && remark == null && projectId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "没有需要更新的内容"));
        }
        String before = snapshot(ledger);

        if (category != null) {
            if (category.isBlank()) throw new DefinedException(new DefaultErrorCode("400", "分类说明不能为空"));
            ledger.setCategory(category.trim());
            if (categoryId == null) ledger.setCategoryNode(null);
        }
        if (categoryId != null) {
            // 兼容旧版 App：分类节点停用后仍可继续更正其他字段，未改选原节点时保留原关联。
            LedgerCategory originalNode = ledger.getCategoryNode();
            LedgerCategory node = originalNode != null && categoryId.equals(originalNode.getCategoryId())
                    ? originalNode
                    : categoryContext.select(current, categoryId, ledger.getDirection());
            ledger.setCategoryNode(node);
            if (category == null || category.isBlank()) ledger.setCategory(node.getName());
        }
        if (purpose != null) ledger.setPurpose(purpose);
        if (counterparty != null) ledger.setCounterparty(counterparty.trim());
        if (summary != null) {
            if (summary.isBlank()) throw new DefinedException(new DefaultErrorCode("400", "摘要不能为空"));
            ledger.setSummary(summary.trim());
        }
        if (remark != null) ledger.setRemark(remark.trim());
        if (projectId != null) {
            Project project = projectId == 0L ? null : accessContext.resolveProject(current, projectId);
            ledger.setProject(project);
            accessContext.applyOwnership(ledger, current, project);
        }

        LedgerManualPurpose effectivePurpose = ledger.getPurpose() == null
                ? LedgerManualPurpose.Other : ledger.getPurpose();
        if (purpose != null || projectId != null) {
            manualContext.validatePurpose(effectivePurpose, ledger.getProject());
        }
        if (category != null || summary != null || purpose != null) {
            manualContext.validateAllowed(ledger.getDirection(), ledger.getCategory(), ledger.getSummary());
        }

        Ledger saved = ledgerRepository.save(ledger);
        LedgerRevision revision = new LedgerRevision();
        revision.setCompany(current.getCompany());
        revision.setLedger(saved);
        revision.setType(LedgerRevisionType.MetadataCorrection);
        revision.setOperatedBy(current);
        revision.setReason(reason.trim());
        revision.setBeforeSnapshot(before);
        revision.setAfterSnapshot(snapshot(saved));
        revisionRepository.save(revision);
        return saved;
    }

    /** 字段未提交或与当前值一致时不算修改；财务补分类时可原样回传表单其他字段。 */
    private boolean changed(String submitted, String current) {
        if (submitted == null) return false;
        String next = submitted.trim();
        String now = current == null ? "" : current.trim();
        return !next.equals(now);
    }

    private boolean projectChanged(Long submitted, Project current) {
        if (submitted == null) return false;
        Long currentId = current == null ? 0L : current.getProjectId();
        return !submitted.equals(currentId);
    }

    private Ledger requireManual(Employee current, Long id) {
        Specification<Ledger> byId = (root, query, builder) ->
                builder.equal(root.get("ledgerId"), id);
        Ledger ledger = ledgerRepository.findOne(byId.and(accessContext.visible(current, Ledger.class)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "手工流水不存在或无权访问")));
        if (ledger.getSource() != LedgerSource.Manual) {
            throw new DefinedException(new DefaultErrorCode("400", "仅手工记账流水允许更正"));
        }
        if (Boolean.TRUE.equals(ledger.getVoided())) {
            throw new DefinedException(new DefaultErrorCode("400", "该流水已作废，不能更正"));
        }
        if (ledger.getCorrectionGroupId() != null) {
            throw new DefinedException(new DefaultErrorCode("400", "该流水已冲正重记，请更正重记后的流水"));
        }
        return ledger;
    }

    private String snapshot(Ledger ledger) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("ledgerId", ledger.getLedgerId());
        value.put("projectId", ledger.getProject() == null ? null : ledger.getProject().getProjectId());
        value.put("direction", ledger.getDirection());
        value.put("categoryId", ledger.getCategoryNode() == null ? null : ledger.getCategoryNode().getCategoryId());
        value.put("category", ledger.getCategory());
        value.put("purpose", ledger.getPurpose());
        value.put("source", ledger.getSource());
        value.put("amount", ledger.getAmount());
        value.put("account", ledger.getAccount());
        value.put("occurredAt", ledger.getOccurredAt());
        value.put("counterparty", ledger.getCounterparty());
        value.put("summary", ledger.getSummary());
        value.put("remark", ledger.getRemark());
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new DefinedException(new DefaultErrorCode("500", "更正快照生成失败"));
        }
    }
}
