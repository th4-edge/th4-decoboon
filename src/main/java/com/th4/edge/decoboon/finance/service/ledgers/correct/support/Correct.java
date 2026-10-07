package com.th4.edge.decoboon.finance.service.ledgers.correct.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
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
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 手工流水冲正重记实现。 */
@Note("手工流水冲正重记")
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
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/ledger/correct")
@Scope(ScopeTypes.PROTOTYPE)
public class Correct implements com.th4.edge.decoboon.finance.service.ledgers.correct.Correct {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long ledgerId;
    @AutoInject
    @NotNull
    private LedgerDirection direction;
    /** 兼容旧版 App 的分类节点；新版本不再传。 */
    @AutoInject
    private Long categoryId;
    @AutoInject
    private LedgerManualPurpose purpose;
    @AutoInject
    @NotEmpty
    @Length(max = 128)
    private String category;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    private FundAccount account;
    @AutoInject
    @NotNull
    private LocalDate occurredAt;
    @AutoInject
    private String counterparty;
    @AutoInject
    @NotEmpty
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

    public Correct(LedgerRepository ledgerRepository, LedgerRevisionRepository revisionRepository,
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
        Employee current = accessContext.requireBoss(employee);
        Ledger original = requireManual(current, ledgerId);
        BigDecimal total = accessContext.value(amount);
        if (total.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "记账金额必须大于 0"));
        }
        Project project = projectId == null ? original.getProject()
                : projectId == 0L ? null : accessContext.resolveProject(current, projectId);
        if (category == null || category.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请填写分类说明"));
        }
        LedgerManualPurpose effectivePurpose = manualContext.purposeOrDefault(purpose);
        manualContext.validatePurpose(effectivePurpose, project);
        manualContext.validateAllowed(direction, category, summary);
        String normalizedCategory = category.trim();
        LedgerCategory categoryNode = null;
        if (categoryId != null && categoryId != 0L) {
            // 兼容旧版 App：仍接受分类节点关联。
            LedgerCategory originalCategoryNode = original.getCategoryNode();
            categoryNode = originalCategoryNode != null && categoryId.equals(originalCategoryNode.getCategoryId())
                    ? originalCategoryNode
                    : categoryContext.select(current, categoryId, direction);
            if (normalizedCategory.isBlank()) normalizedCategory = categoryNode.getName();
        }

        String groupId = UUID.randomUUID().toString();
        Ledger reversal = new Ledger();
        reversal.setCompany(original.getCompany());
        reversal.setStore(original.getStore());
        reversal.setTeam(original.getTeam());
        reversal.setOwner(original.getOwner());
        reversal.setProject(original.getProject());
        reversal.setDirection(opposite(original.getDirection()));
        reversal.setCategory(original.getCategory());
        reversal.setCategoryNode(original.getCategoryNode());
        reversal.setPurpose(original.getPurpose());
        reversal.setSource(LedgerSource.Adjustment);
        reversal.setAmount(accessContext.value(original.getAmount()));
        reversal.setAccount(original.getAccount());
        reversal.setOccurredAt(original.getOccurredAt());
        reversal.setCounterparty(original.getCounterparty());
        String originalSummary = original.getSummary() == null ? "" : original.getSummary();
        String reversalSummary = "更正冲销：" + originalSummary;
        reversal.setSummary(reversalSummary.length() <= 256 ? reversalSummary : reversalSummary.substring(0, 256));
        reversal.setRemark(reason.trim());
        reversal.setCreatedBy(current);
        reversal.setOriginalLedger(original);
        reversal.setCorrectionGroupId(groupId);
        reversal = ledgerRepository.save(reversal);

        Ledger replacement = new Ledger();
        replacement.setDirection(direction);
        replacement.setCategory(normalizedCategory);
        replacement.setCategoryNode(categoryNode);
        replacement.setPurpose(effectivePurpose);
        replacement.setSource(LedgerSource.Manual);
        replacement.setAmount(total);
        replacement.setAccount(account == null ? original.getAccount() : account);
        replacement.setOccurredAt(occurredAt);
        replacement.setCounterparty(counterparty == null ? null : counterparty.trim());
        replacement.setSummary(summary.trim());
        replacement.setRemark(remark == null ? null : remark.trim());
        replacement.setCreatedBy(current);
        replacement.setOriginalLedger(original);
        replacement.setCorrectionGroupId(groupId);
        accessContext.applyOwnership(replacement, current, project);
        replacement = ledgerRepository.save(replacement);

        original.setCorrectionGroupId(groupId);
        ledgerRepository.save(original);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("reversalLedgerId", reversal.getLedgerId());
        after.put("correctedLedgerId", replacement.getLedgerId());
        after.put("direction", replacement.getDirection());
        after.put("categoryId", replacement.getCategoryNode() == null ? null : replacement.getCategoryNode().getCategoryId());
        after.put("category", replacement.getCategory());
        after.put("purpose", replacement.getPurpose());
        after.put("amount", replacement.getAmount());
        after.put("account", replacement.getAccount());
        after.put("occurredAt", replacement.getOccurredAt());
        after.put("counterparty", replacement.getCounterparty());
        after.put("summary", replacement.getSummary());
        after.put("remark", replacement.getRemark());
        after.put("projectId", replacement.getProject() == null ? null : replacement.getProject().getProjectId());

        LedgerRevision revision = new LedgerRevision();
        revision.setCompany(current.getCompany());
        revision.setLedger(original);
        revision.setType(LedgerRevisionType.ReverseAndRecreate);
        revision.setOperatedBy(current);
        revision.setReason(reason.trim());
        revision.setBeforeSnapshot(snapshot(original));
        revision.setAfterSnapshot(json(after));
        revisionRepository.save(revision);
        return replacement;
    }

    private Ledger requireManual(Employee current, Long id) {
        Ledger ledger = ledgerRepository.findByCompany_ComIdAndLedgerId(current.getCompany().getComId(), id)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "手工流水不存在或无权访问")));
        if (ledger.getSource() != LedgerSource.Manual) {
            throw new DefinedException(new DefaultErrorCode("400", "仅手工记账流水允许冲正重记"));
        }
        if (Boolean.TRUE.equals(ledger.getVoided())) {
            throw new DefinedException(new DefaultErrorCode("400", "该流水已作废，不能冲正重记"));
        }
        if (ledger.getCorrectionGroupId() != null) {
            throw new DefinedException(new DefaultErrorCode("400", "该流水已冲正重记，请更正重记后的流水"));
        }
        return ledger;
    }

    private LedgerDirection opposite(LedgerDirection direction) {
        return direction == LedgerDirection.Income ? LedgerDirection.Expense : LedgerDirection.Income;
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
        return json(value);
    }

    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new DefinedException(new DefaultErrorCode("500", "更正快照生成失败"));
        }
    }
}
