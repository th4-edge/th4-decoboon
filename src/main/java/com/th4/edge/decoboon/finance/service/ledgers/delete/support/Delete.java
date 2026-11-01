package com.th4.edge.decoboon.finance.service.ledgers.delete.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.LedgerRevisionType;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Ledger;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** 手工流水逻辑作废实现：不物理删除，保留审计与凭证依据。 */
@Note("作废手工流水")
@Properties(allow = {
        @AllowProperties(model = Ledger.class,
                properties = {"ledgerId", "project", "direction", "category", "categoryNode", "purpose", "source", "amount", "account",
                        "occurredAt", "counterparty", "summary", "remark", "voided", "voidedBy", "voidedAt", "voidReason",
                        "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.finance.model.LedgerCategory.class,
                properties = {"categoryId", "name", "description", "direction", "code", "builtIn", "sort", "active"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class, properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"})
})
@Service("finance/ledger/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.finance.service.ledgers.delete.Delete {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long ledgerId;
    @AutoInject
    @NotEmpty
    @Length(max = 500)
    private String reason;

    private final LedgerRepository ledgerRepository;
    private final LedgerRevisionRepository revisionRepository;
    private final FinanceAccessContext accessContext;
    private final ObjectMapper objectMapper;

    public Delete(LedgerRepository ledgerRepository, LedgerRevisionRepository revisionRepository,
                  FinanceAccessContext accessContext, ObjectMapper objectMapper) {
        this.ledgerRepository = ledgerRepository;
        this.revisionRepository = revisionRepository;
        this.accessContext = accessContext;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Ledger call(Ledger request) {
        Employee current = accessContext.requireHighRiskEditor(employee);
        Ledger ledger = ledgerRepository.findByCompany_ComIdAndLedgerId(current.getCompany().getComId(), ledgerId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "手工流水不存在或无权访问")));
        accessContext.requireLedgerScope(current, ledger);
        if (ledger.getSource() != LedgerSource.Manual) {
            throw new DefinedException(new DefaultErrorCode("400", "仅手工记账流水允许作废"));
        }
        if (ledger.getCorrectionGroupId() != null) {
            throw new DefinedException(new DefaultErrorCode("400", "该流水已冲正重记，不能作废"));
        }
        if (Boolean.TRUE.equals(ledger.getVoided())) {
            throw new DefinedException(new DefaultErrorCode("400", "该流水已作废"));
        }
        String before = snapshot(ledger);
        ledger.setVoided(Boolean.TRUE);
        ledger.setVoidedBy(current);
        ledger.setVoidedAt(LocalDateTime.now());
        ledger.setVoidReason(reason.trim());
        Ledger saved = ledgerRepository.save(ledger);

        LedgerRevision revision = new LedgerRevision();
        revision.setCompany(current.getCompany());
        revision.setLedger(saved);
        revision.setType(LedgerRevisionType.Void);
        revision.setOperatedBy(current);
        revision.setReason(reason.trim());
        revision.setBeforeSnapshot(before);
        revision.setAfterSnapshot(snapshot(saved));
        revisionRepository.save(revision);
        return saved;
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
        value.put("voided", ledger.getVoided());
        value.put("voidReason", ledger.getVoidReason());
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new DefinedException(new DefaultErrorCode("500", "作废快照生成失败"));
        }
    }
}
