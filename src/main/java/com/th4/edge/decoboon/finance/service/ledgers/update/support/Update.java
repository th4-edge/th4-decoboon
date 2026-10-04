package com.th4.edge.decoboon.finance.service.ledgers.update.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.finance.configure.LedgerRevisionType;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.LedgerCategoryContext;
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
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** 手工流水元数据更正实现。 */
@Note("更正手工流水元数据")
@Properties(allow = @AllowProperties(model = Ledger.class,
        properties = {"ledgerId", "project", "direction", "category", "categoryNode", "source", "amount", "account",
                "occurredAt", "counterparty", "summary", "remark", "originalLedger", "correctionGroupId", "createdBy", "createTime"}))
@Service("finance/ledger/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.finance.service.ledgers.update.Update {

    @User
    private Employee employee;
    @AutoInject
    private Long ledgerId;
    @AutoInject
    private Long categoryId;
    @AutoInject
    @Length(max = 128)
    private String category;
    @AutoInject
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
    private final FinanceAccessContext accessContext;
    private final ObjectMapper objectMapper;

    public Update(LedgerRepository ledgerRepository, LedgerRevisionRepository revisionRepository,
                  LedgerCategoryContext categoryContext, FinanceAccessContext accessContext,
                  ObjectMapper objectMapper) {
        this.ledgerRepository = ledgerRepository;
        this.revisionRepository = revisionRepository;
        this.categoryContext = categoryContext;
        this.accessContext = accessContext;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Ledger call(Ledger request) {
        Employee current = accessContext.requireBoss(employee);
        Ledger ledger = requireManual(current, ledgerId);
        String before = snapshot(ledger);
        if (categoryId != null) {
            // 分类停用后，历史流水仍可继续更正摘要等项目；未改选原分类时保留原关联，避免拆断历史。
            LedgerCategory originalNode = ledger.getCategoryNode();
            LedgerCategory node = originalNode != null && categoryId.equals(originalNode.getCategoryId())
                    ? originalNode
                    : categoryContext.select(current, categoryId, ledger.getDirection());
            ledger.setCategoryNode(node);
            if (node != null && (category == null || category.isBlank())) {
                ledger.setCategory(node.getName());
            }
        }
        if (category != null) {
            if (category.isBlank()) throw new DefinedException(new DefaultErrorCode("400", "业务分类不能为空"));
            ledger.setCategory(category.trim());
        }
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

    private Ledger requireManual(Employee current, Long id) {
        Ledger ledger = ledgerRepository.findByCompany_ComIdAndLedgerId(current.getCompany().getComId(), id)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "手工流水不存在或无权访问")));
        if (ledger.getSource() != LedgerSource.Manual) {
            throw new DefinedException(new DefaultErrorCode("400", "仅手工记账流水允许更正"));
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
